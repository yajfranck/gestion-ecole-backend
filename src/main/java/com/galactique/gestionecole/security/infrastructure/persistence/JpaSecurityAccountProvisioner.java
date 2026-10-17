package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.domain.PasswordChangeResult;

@Service
public class JpaSecurityAccountProvisioner implements SecurityAccountProvisioner {
	private static final String EMPLOYEE_SECURITY_ACCOUNT_NOT_FOUND = "Employee security account was not found";

	private final UserAccountJpaRepository userAccountJpaRepository;
	private final RoleJpaRepository roleJpaRepository;
	private final RefreshTokenJpaRepository refreshTokenJpaRepository;
	private final PasswordEncoder passwordEncoder;

	public JpaSecurityAccountProvisioner(
		UserAccountJpaRepository userAccountJpaRepository,
		RoleJpaRepository roleJpaRepository,
		RefreshTokenJpaRepository refreshTokenJpaRepository,
		PasswordEncoder passwordEncoder) {
		this.userAccountJpaRepository = userAccountJpaRepository;
		this.roleJpaRepository = roleJpaRepository;
		this.refreshTokenJpaRepository = refreshTokenJpaRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public PasswordChangeResult changePassword(String username, String currentPassword, String newPassword) {
		UserAccountEntity account = userAccountJpaRepository.findByUsernameForUpdate(username)
			.orElse(null);
		if (account == null || !account.isEnabled() || account.isLocked()
			|| !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
			return PasswordChangeResult.CURRENT_PASSWORD_INVALID;
		}
		if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
			return PasswordChangeResult.NEW_PASSWORD_UNCHANGED;
		}
		account.changePasswordHash(passwordEncoder.encode(newPassword));
		refreshTokenJpaRepository.revokeActiveByUserAccountId(account.getId(), Instant.now());
		return PasswordChangeResult.UPDATED;
	}

	@Override
	@Transactional
	public UUID createGuardianAccount(String username, String rawPassword) {
		if (userAccountJpaRepository.findByUsernameIgnoreCase(username).isPresent()) {
			throw new DataIntegrityViolationException("A security account already exists for this username");
		}
		RoleEntity parentRole = roleJpaRepository.findByCode("PARENT")
			.filter(role -> role.isEnabled())
			.orElseThrow(() -> new IllegalStateException("Required security role is missing: PARENT"));
		UserAccountEntity account = new UserAccountEntity(
			username.strip().toLowerCase(Locale.ROOT), passwordEncoder.encode(rawPassword), Set.of(parentRole));
		return userAccountJpaRepository.save(account).getId();
	}

	@Override
	@Transactional
	public void addGuardianRole(UUID userAccountId) {
		UserAccountEntity account = userAccountJpaRepository.findByIdForUpdate(userAccountId)
			.orElseThrow(() -> new IllegalStateException("Guardian security account was not found"));
		RoleEntity parentRole = roleJpaRepository.findByCode("PARENT")
			.filter(RoleEntity::isEnabled)
			.orElseThrow(() -> new IllegalStateException("Required security role is missing: PARENT"));
		account.addRole(parentRole);
	}

	@Override
	@Transactional
	public UUID createEmployeeAccount(String username, String rawPassword, Set<String> effectiveRoleCodes) {
		if (userAccountJpaRepository.findByUsernameIgnoreCase(username).isPresent()) {
			throw new DataIntegrityViolationException("A security account already exists for this username");
		}

		UserAccountEntity userAccount = new UserAccountEntity(
			username,
			passwordEncoder.encode(rawPassword),
			resolveEmployeeRoles(effectiveRoleCodes));
		return userAccountJpaRepository.save(userAccount).getId();
	}

	@Override
	@Transactional
	public void updateEmployeeAccount(UUID userAccountId, String username, Set<String> effectiveRoleCodes) {
		String normalizedUsername = username.strip().toLowerCase(Locale.ROOT);
		userAccountJpaRepository.findByUsernameIgnoreCase(normalizedUsername).ifPresent(existing -> {
			if (!existing.getId().equals(userAccountId)) {
				throw new DataIntegrityViolationException("A security account already exists for this username");
			}
		});
		UserAccountEntity userAccount = userAccountJpaRepository.findByIdForUpdate(userAccountId)
			.orElseThrow(() -> new IllegalStateException(EMPLOYEE_SECURITY_ACCOUNT_NOT_FOUND));
		userAccount.updateEmployeeAccount(normalizedUsername, resolveEmployeeRoles(effectiveRoleCodes));
	}

	@Override
	@Transactional
	public void updateEmployeeRoles(UUID userAccountId, Set<String> effectiveRoleCodes) {
		UserAccountEntity userAccount = userAccountJpaRepository.findByIdForUpdate(userAccountId)
			.orElseThrow(() -> new IllegalStateException(EMPLOYEE_SECURITY_ACCOUNT_NOT_FOUND));
		userAccount.updateEmployeeAccount(userAccount.getUsername(), resolveEmployeeRoles(effectiveRoleCodes));
	}

	@Override
	@Transactional
	public void disableEmployeeAccount(UUID userAccountId) {
		UserAccountEntity userAccount = userAccountJpaRepository.findById(userAccountId)
			.orElseThrow(() -> new IllegalStateException(EMPLOYEE_SECURITY_ACCOUNT_NOT_FOUND));
		userAccount.disable();
		refreshTokenJpaRepository.revokeActiveByUserAccountId(userAccountId, Instant.now());
	}

	private Set<RoleEntity> resolveEmployeeRoles(Set<String> roleCodes) {
		if (roleCodes.isEmpty() || roleCodes.contains("ADMIN") || roleCodes.contains("PARENT")) {
			throw new IllegalArgumentException("Employee accounts cannot receive reserved ADMIN or PARENT roles");
		}
		Set<RoleEntity> roles = new LinkedHashSet<>();
		for (String roleCode : roleCodes) {
			RoleEntity role = roleJpaRepository.findByCode(roleCode)
				.orElseThrow(() -> new IllegalStateException("Required security role is missing: " + roleCode));
			if (!role.isEnabled()) {
				throw new IllegalStateException("Required security role is disabled: " + roleCode);
			}
			roles.add(role);
		}
		return roles;
	}
}
