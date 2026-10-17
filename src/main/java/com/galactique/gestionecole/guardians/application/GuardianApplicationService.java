package com.galactique.gestionecole.guardians.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.guardians.domain.Guardian;
import com.galactique.gestionecole.guardians.domain.GuardianInvitation;
import com.galactique.gestionecole.guardians.domain.GuardianInvitationRepository;
import com.galactique.gestionecole.guardians.domain.GuardianRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;

@Service
public class GuardianApplicationService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int INVITATION_TOKEN_BYTES = 32;
	private static final int INVITATION_TTL_HOURS = 48;

	private final GuardianRepository guardianRepository;
	private final GuardianInvitationRepository invitationRepository;
	private final StudentRepository studentRepository;
	private final EmployeeApplicationService employeeApplicationService;
	private final SecurityAccountProvisioner securityAccountProvisioner;
	private final UserAccountRepository userAccountRepository;
	private final GuardianInvitationMailer invitationMailer;
	private final LicenseApplicationService licenseApplicationService;

	public GuardianApplicationService(GuardianRepository guardianRepository,
		GuardianInvitationRepository invitationRepository, StudentRepository studentRepository,
		EmployeeApplicationService employeeApplicationService,
		SecurityAccountProvisioner securityAccountProvisioner, UserAccountRepository userAccountRepository,
		GuardianInvitationMailer invitationMailer, LicenseApplicationService licenseApplicationService) {
		this.guardianRepository = guardianRepository;
		this.invitationRepository = invitationRepository;
		this.studentRepository = studentRepository;
		this.employeeApplicationService = employeeApplicationService;
		this.securityAccountProvisioner = securityAccountProvisioner;
		this.userAccountRepository = userAccountRepository;
		this.invitationMailer = invitationMailer;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Transactional
	public GuardianLinkResult linkToStudent(UUID establishmentId, UUID studentId,
		String firstName, String lastName, String email, EmployeeActor actor) {
		requireAccessibleStudent(establishmentId, studentId, actor);
		String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
		var existingAccount = userAccountRepository.findByUsername(normalizedEmail);
		Guardian guardian = existingAccount.flatMap(account -> guardianRepository.findByUserAccountId(account.id()))
			.or(() -> guardianRepository.findByEmail(normalizedEmail)).orElseGet(() -> {
			try {
				return guardianRepository.save(firstName.strip(), lastName.strip(), normalizedEmail);
			} catch (DataIntegrityViolationException exception) {
				throw new GuardianConflictException();
			}
		});
		if (!guardian.enabled()) {
			throw new GuardianNotFoundException();
		}
		if (guardian.userAccountId() != null && existingAccount.isPresent()
			&& !guardian.userAccountId().equals(existingAccount.orElseThrow().id())) {
			throw new GuardianConflictException();
		}
		if (guardianRepository.existsLink(guardian.id(), studentId)) {
			throw new GuardianConflictException();
		}
		try {
			guardianRepository.link(guardian.id(), studentId);
		} catch (DataIntegrityViolationException exception) {
			throw new GuardianConflictException();
		}
		if (guardian.userAccountId() != null) {
			return new GuardianLinkResult(guardian, "ACCESS_ALREADY_ACTIVE");
		}
		if (existingAccount.isPresent()) {
			UUID accountId = existingAccount.orElseThrow().id();
			securityAccountProvisioner.addGuardianRole(accountId);
			Guardian attachedGuardian = guardianRepository.attachUserAccount(guardian.id(), accountId);
			return new GuardianLinkResult(attachedGuardian, "ACCESS_ALREADY_ACTIVE");
		}

		String token = generateToken();
		Instant now = Instant.now();
		invitationRepository.save(new GuardianInvitation(UUID.randomUUID(), guardian.id(), hashToken(token),
			now, now.plus(INVITATION_TTL_HOURS, ChronoUnit.HOURS), null));
		invitationMailer.sendInvitation(guardian, token);
		return new GuardianLinkResult(guardian, "INVITATION_SENT");
	}

	@Transactional(readOnly = true)
	public List<Guardian> findByStudent(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		requireAccessibleStudent(establishmentId, studentId, actor);
		return guardianRepository.findAllByStudentId(studentId);
	}

	@Transactional
	public void unlinkFromStudent(UUID establishmentId, UUID studentId, UUID guardianId, EmployeeActor actor) {
		requireAccessibleStudent(establishmentId, studentId, actor);
		if (!guardianRepository.existsLink(guardianId, studentId)) {
			throw new GuardianNotFoundException();
		}
		guardianRepository.unlink(guardianId, studentId);
	}

	@Transactional
	public void acceptInvitation(String token, String rawPassword) {
		if (token == null || token.isBlank() || token.length() > 100 || rawPassword == null || rawPassword.isBlank()
			|| rawPassword.length() < 12 || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new InvalidGuardianInvitationException();
		}
		Instant now = Instant.now();
		GuardianInvitation invitation = invitationRepository.findByTokenHashForUpdate(hashToken(token))
			.orElseThrow(InvalidGuardianInvitationException::new);
		if (invitation.isAccepted() || !invitation.expiresAt().isAfter(now)) {
			throw new InvalidGuardianInvitationException();
		}
		Guardian guardian = guardianRepository.findByIdForUpdate(invitation.guardianId())
			.orElseThrow(InvalidGuardianInvitationException::new);
		if (!guardian.enabled() || guardian.userAccountId() != null) {
			throw new InvalidGuardianInvitationException();
		}
		try {
			UUID accountId = securityAccountProvisioner.createGuardianAccount(guardian.email(), rawPassword);
			guardianRepository.attachUserAccount(guardian.id(), accountId);
			invitationRepository.markAccepted(new GuardianInvitation(invitation.id(), invitation.guardianId(),
				invitation.tokenHash(), invitation.createdAt(), invitation.expiresAt(), now));
		} catch (DataIntegrityViolationException exception) {
			throw new GuardianInvitationConflictException();
		}
	}

	@Transactional(readOnly = true)
	public List<Student> findChildren(String username) {
		return userAccountRepository.findByUsername(username)
			.filter(account -> account.enabled() && !account.locked())
			.flatMap(account -> guardianRepository.findByUserAccountId(account.id()))
			.filter(guardian -> guardian.enabled())
			.filter(guardian -> guardian.userAccountId() != null)
			.map(guardian -> guardianRepository.findChildren(guardian.id()).stream()
				.filter(student -> licenseApplicationService.hasFeature(student.establishmentId(),
					LicenseFeature.PARENT_PORTAL, Instant.now()))
				.toList())
			.orElseGet(List::of);
	}

	@Transactional(readOnly = true)
	public Student findLinkedChild(String username, UUID studentId) {
		Guardian guardian = userAccountRepository.findByUsername(username)
			.filter(account -> account.enabled() && !account.locked())
			.flatMap(account -> guardianRepository.findByUserAccountId(account.id()))
			.filter(Guardian::enabled)
			.filter(item -> item.userAccountId() != null)
			.orElseThrow(GuardianNotFoundException::new);
		return guardianRepository.findLinkedChild(guardian.id(), studentId)
			.orElseThrow(GuardianNotFoundException::new);
	}

	private void requireAccessibleStudent(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		List<Establishment> establishments;
		try {
			establishments = employeeApplicationService.findEstablishments(actor);
		} catch (EmployeeAccessDeniedException exception) {
			throw new GuardianAccessDeniedException();
		}
		if (establishments.stream().noneMatch(establishment -> establishment.enabled()
			&& establishment.id().equals(establishmentId))) {
			throw new GuardianNotFoundException();
		}
		studentRepository.findByIdAndEstablishmentId(studentId, establishmentId)
			.orElseThrow(GuardianNotFoundException::new);
	}

	private String generateToken() {
		byte[] token = new byte[INVITATION_TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
	}

	private String hashToken(String token) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	public record GuardianLinkResult(Guardian guardian, String status) {
	}
}
