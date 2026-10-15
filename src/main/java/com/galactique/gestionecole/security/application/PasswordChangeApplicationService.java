package com.galactique.gestionecole.security.application;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.security.domain.PasswordChangeResult;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;

@Service
public class PasswordChangeApplicationService {
	private static final int MIN_PASSWORD_LENGTH = 12;
	private static final int MAX_BCRYPT_BYTES = 72;

	private final SecurityAccountProvisioner securityAccountProvisioner;

	public PasswordChangeApplicationService(SecurityAccountProvisioner securityAccountProvisioner) {
		this.securityAccountProvisioner = securityAccountProvisioner;
	}

	@Transactional
	public void changePassword(String username, String currentPassword, String newPassword) {
		if (currentPassword == null || currentPassword.isBlank()) {
			throw new IncorrectCurrentPasswordException();
		}
		if (newPassword == null || newPassword.isBlank()
			|| newPassword.length() < MIN_PASSWORD_LENGTH
			|| newPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
			throw new InvalidNewPasswordException("New password must contain at least 12 characters and at most 72 UTF-8 bytes");
		}

		PasswordChangeResult result = securityAccountProvisioner.changePassword(
			username, currentPassword, newPassword);
		if (result == PasswordChangeResult.CURRENT_PASSWORD_INVALID) {
			throw new IncorrectCurrentPasswordException();
		}
		if (result == PasswordChangeResult.NEW_PASSWORD_UNCHANGED) {
			throw new InvalidNewPasswordException("New password must be different from the current password");
		}
	}
}