package com.galactique.gestionecole.security.infrastructure.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.security.application.PasswordChangeApplicationService;
import com.galactique.gestionecole.security.infrastructure.api.dto.ChangePasswordRequest;
import jakarta.validation.Valid;

@RestController
public class PasswordController {
	private final PasswordChangeApplicationService passwordChangeApplicationService;

	public PasswordController(PasswordChangeApplicationService passwordChangeApplicationService) {
		this.passwordChangeApplicationService = passwordChangeApplicationService;
	}

	@PutMapping("/api/v1/user/me/password")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
		passwordChangeApplicationService.changePassword(
			authentication.getName(), request.currentPassword(), request.newPassword());
	}
}