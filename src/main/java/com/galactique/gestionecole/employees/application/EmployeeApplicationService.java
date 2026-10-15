package com.galactique.gestionecole.employees.application;

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
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.establishments.application.EstablishmentEmployeeDeactivation;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.employees.domain.EmployeeInvitation;
import com.galactique.gestionecole.employees.domain.EmployeeInvitationRepository;
import com.galactique.gestionecole.positions.domain.Position;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;

@Service
public class EmployeeApplicationService implements EstablishmentEmployeeDeactivation {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int INVITATION_TOKEN_BYTES = 32;
	private static final int INVITATION_TTL_HOURS = 48;

	private final EstablishmentRepository establishmentRepository;
	private final PositionRepository positionRepository;
	private final EmployeeRepository employeeRepository;
	private final EmployeeInvitationRepository invitationRepository;
	private final EmployeeInvitationMailer invitationMailer;
	private final SecurityAccountProvisioner securityAccountProvisioner;

	public EmployeeApplicationService(
		EstablishmentRepository establishmentRepository,
		PositionRepository positionRepository,
		EmployeeRepository employeeRepository,
		EmployeeInvitationRepository invitationRepository,
		EmployeeInvitationMailer invitationMailer,
		SecurityAccountProvisioner securityAccountProvisioner) {
		this.establishmentRepository = establishmentRepository;
		this.positionRepository = positionRepository;
		this.employeeRepository = employeeRepository;
		this.invitationRepository = invitationRepository;
		this.invitationMailer = invitationMailer;
		this.securityAccountProvisioner = securityAccountProvisioner;
	}

	@Transactional
	public Employee invite(CreateEmployeeCommand command, EmployeeActor actor) {
		Position position = positionRepository.findById(command.positionId())
			.filter(Position::enabled)
			.orElseThrow(EmployeeNotFoundException::new);
		if (!actor.globalAdministrator()
			&& !requireEstablishmentAdministrator(actor).establishmentId().equals(position.establishmentId())) {
			throw new EmployeeAccessDeniedException();
		}
		var establishment = establishmentRepository.findById(position.establishmentId())
			.orElseThrow(EmployeeNotFoundException::new);
		if (!establishment.enabled()) {
			throw new EmployeeNotFoundException();
		}
		Set<String> directRoleCodes = validateDirectRoleCodes(command.directRoleCodes(), actor);
		Set<String> effectiveRoleCodes = effectiveRoleCodes(directRoleCodes, position.roleCodes());
		if (effectiveRoleCodes.isEmpty()) {
			throw new InvalidEmployeeRolesException();
		}
		if (employeeRepository.existsByEmployeeNumber(command.employeeNumber())
			|| employeeRepository.existsByEmail(command.email())) {
			throw new EmployeeConflictException();
		}

		Employee employee;
		try {
			employee = employeeRepository.save(command.employeeNumber(), command.firstName(), command.lastName(),
			command.email(), position.id(), directRoleCodes);
		} catch (DataIntegrityViolationException exception) {
			throw new EmployeeConflictException();
		}

		String invitationToken = generateToken();
		Instant now = Instant.now();
		invitationRepository.save(new EmployeeInvitation(
			UUID.randomUUID(), employee.id(), hashToken(invitationToken), now,
			now.plus(INVITATION_TTL_HOURS, ChronoUnit.HOURS), null));
		invitationMailer.sendInvitation(employee, invitationToken);
		return employee;
	}

	@Transactional
	public void acceptInvitation(String token, String rawPassword) {
		if (token == null || token.isBlank() || token.length() > 100
			|| rawPassword == null || rawPassword.isBlank()
			|| rawPassword.length() < 12
			|| rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new InvalidEmployeeInvitationException();
		}

		Instant now = Instant.now();
		EmployeeInvitation invitation = invitationRepository.findByTokenHashForUpdate(hashToken(token))
			.orElseThrow(InvalidEmployeeInvitationException::new);
		if (invitation.isAccepted() || !invitation.expiresAt().isAfter(now)) {
			throw new InvalidEmployeeInvitationException();
		}

		Employee employee = employeeRepository.findByIdForUpdate(invitation.employeeId())
			.orElseThrow(InvalidEmployeeInvitationException::new);
		if (!employee.enabled() || employee.userAccountId() != null) {
			throw new InvalidEmployeeInvitationException();
		}
		Position position = positionRepository.findByIdAndEstablishmentId(
			employee.positionId(), employee.establishmentId())
			.filter(Position::enabled)
			.orElseThrow(InvalidEmployeeInvitationException::new);
		Set<String> effectiveRoleCodes = effectiveRoleCodes(employee.directRoleCodes(), position.roleCodes());
		if (effectiveRoleCodes.isEmpty()) {
			throw new InvalidEmployeeInvitationException();
		}

		UUID userAccountId = securityAccountProvisioner.createEmployeeAccount(
			employee.email(), rawPassword, effectiveRoleCodes);
		employeeRepository.attachUserAccount(employee.id(), userAccountId);
		invitationRepository.markAccepted(new EmployeeInvitation(
			invitation.id(), invitation.employeeId(), invitation.tokenHash(),
			invitation.createdAt(), invitation.expiresAt(), now));
	}

	@Transactional(readOnly = true)
	public List<Employee> findAll(EmployeeActor actor) {
		if (actor.globalAdministrator()) {
			return employeeRepository.findAll();
		}
		Employee actorEmployee = requireEstablishmentAdministrator(actor);
		return employeeRepository.findAllByEstablishmentId(actorEmployee.establishmentId());
	}

	@Transactional(readOnly = true)
	public Employee findById(UUID id, EmployeeActor actor) {
		return findWithinScope(id, actor);
	}

	private Employee findWithinScope(UUID id, EmployeeActor actor) {
		Employee employee = employeeRepository.findById(id).orElseThrow(EmployeeNotFoundException::new);
		if (!actor.globalAdministrator()) {
			Employee actorEmployee = requireEstablishmentAdministrator(actor);
			if (!actorEmployee.establishmentId().equals(employee.establishmentId())) {
				throw new EmployeeNotFoundException();
			}
		}
		return employee;
	}

	@Transactional
	public Employee updateProfile(UUID id, UpdateEmployeeCommand command, EmployeeActor actor) {
		Employee current = findWithinScope(id, actor);
		if (!current.enabled()) {
			throw new EmployeeNotFoundException();
		}
		String employeeNumber = command.employeeNumber() == null ? current.employeeNumber() : command.employeeNumber();
		String firstName = command.firstName() == null ? current.firstName() : command.firstName();
		String lastName = command.lastName() == null ? current.lastName() : command.lastName();
		String email = command.email() == null ? current.email() : command.email().strip().toLowerCase(Locale.ROOT);
		UUID positionId = command.positionId() == null ? current.positionId() : command.positionId();
		Position position = positionRepository.findByIdAndEstablishmentId(positionId, current.establishmentId())
			.filter(Position::enabled)
			.orElseThrow(EmployeeNotFoundException::new);

		if (employeeRepository.existsByEmployeeNumberAndIdNot(employeeNumber, id)
			|| employeeRepository.existsByEmailAndIdNot(email, id)) {
			throw new EmployeeConflictException();
		}

		Employee updated = employeeRepository.updateProfile(id, employeeNumber, firstName, lastName, email, positionId);
		if (updated.userAccountId() != null
			&& (!current.email().equals(updated.email()) || !current.positionId().equals(updated.positionId()))) {
			securityAccountProvisioner.updateEmployeeAccount(
				updated.userAccountId(), updated.email(), effectiveRoleCodes(updated.directRoleCodes(), position.roleCodes()));
		}
		return updated;
	}

	@Transactional
	public Employee updateRoles(UUID id, List<String> roleCodes, EmployeeActor actor) {
		Employee current = findWithinScope(id, actor);
		if (!current.enabled()) {
			throw new EmployeeNotFoundException();
		}
		Set<String> requestedRoles = validateDirectRoleCodes(roleCodes == null ? Set.of() : Set.copyOf(roleCodes), actor);
		Position position = positionRepository.findByIdAndEstablishmentId(current.positionId(), current.establishmentId())
			.filter(Position::enabled)
			.orElseThrow(EmployeeNotFoundException::new);
		Set<String> effectiveRoleCodes = effectiveRoleCodes(requestedRoles, position.roleCodes());
		if (effectiveRoleCodes.isEmpty()) {
			throw new InvalidEmployeeRolesException();
		}
		Employee updated = employeeRepository.updateRoles(id, requestedRoles);
		if (updated.userAccountId() != null) {
			securityAccountProvisioner.updateEmployeeRoles(updated.userAccountId(), effectiveRoleCodes);
		}
		return updated;
	}

	@Transactional
	public void deactivateEmployee(UUID id, EmployeeActor actor) {
		Employee current = findWithinScope(id, actor);
		employeeRepository.disable(id);
		if (current.userAccountId() != null) {
			securityAccountProvisioner.disableEmployeeAccount(current.userAccountId());
		}
	}

	@Override
	@Transactional
	public void disableEmployeeForEstablishment(UUID establishmentId) {
		for (Employee employee : employeeRepository.findAllByEstablishmentId(establishmentId)) {
			if (employee.enabled()) {
				employeeRepository.disable(employee.id());
				if (employee.userAccountId() != null) {
					securityAccountProvisioner.disableEmployeeAccount(employee.userAccountId());
				}
			}
		}
	}

	@Transactional(readOnly = true)
	public Employee findCurrent(String username) {
		Employee employee = employeeRepository.findByEmail(username).orElseThrow(EmployeeNotFoundException::new);
		if (!employee.enabled()) {
			throw new EmployeeNotFoundException();
		}
		return employee;
	}

	@Transactional(readOnly = true)
	public List<com.galactique.gestionecole.establishments.domain.Establishment> findEstablishments(EmployeeActor actor) {
		if (actor.globalAdministrator()) {
			return establishmentRepository.findAll();
		}
		Employee actorEmployee = requireEstablishmentAdministrator(actor);
		return establishmentRepository.findById(actorEmployee.establishmentId()).stream().toList();
	}

	private Employee requireEstablishmentAdministrator(EmployeeActor actor) {
		if (!actor.establishmentAdministrator()) {
			throw new EmployeeAccessDeniedException();
		}
		Employee employee = employeeRepository.findByEmail(actor.username())
			.orElseThrow(EmployeeAccessDeniedException::new);
		if (!employee.enabled() || !employee.establishmentAdmin() || employee.userAccountId() == null) {
			throw new EmployeeAccessDeniedException();
		}
		return employee;
	}

	private Set<String> validateDirectRoleCodes(Set<String> roleCodes, EmployeeActor actor) {
		Set<String> normalized = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
		if (!Set.of("STAFF", "ESTABLISHMENT_ADMIN").containsAll(normalized)
			|| normalized.contains("ESTABLISHMENT_ADMIN") && !actor.globalAdministrator()) {
			throw new InvalidEmployeeRolesException();
		}
		return normalized;
	}

	private Set<String> effectiveRoleCodes(Set<String> directRoleCodes, Set<String> positionRoleCodes) {
		Set<String> effectiveRoleCodes = new java.util.LinkedHashSet<>(directRoleCodes);
		effectiveRoleCodes.addAll(positionRoleCodes);
		return Set.copyOf(effectiveRoleCodes);
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
}