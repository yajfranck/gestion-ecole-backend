package com.galactique.gestionecole.security.infrastructure.api.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.security.application.CallerScopeService;
import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.domain.UserAccount;
import com.galactique.gestionecole.security.infrastructure.api.dto.PermissionResponse;
import com.galactique.gestionecole.security.infrastructure.api.dto.RoleResponse;
import com.galactique.gestionecole.security.infrastructure.api.dto.UserAccountResponse;

@RestController
@RequestMapping("/api/v1")
public class SecurityDirectoryController {
	private final CallerScopeService callerScope;
	private final SecurityDirectoryRepository securityDirectoryRepository;

	public SecurityDirectoryController(SecurityDirectoryRepository securityDirectoryRepository, CallerScopeService callerScope) {
		this.callerScope = callerScope;
		this.securityDirectoryRepository = securityDirectoryRepository;
	}

	/** Retourne les comptes utilisateurs et leurs rôles. */
	@PreAuthorize("hasAuthority('can_user_read')")
	@GetMapping("/user")
	public List<UserAccountResponse> getUsers(Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return securityDirectoryRepository.findAllUsers().stream()
			.sorted(Comparator.comparing(UserAccount::username))
			.map(user -> new UserAccountResponse(
				user.id(),
				user.username(),
				user.enabled(),
				user.locked(),
				user.createdAt(),
				user.updatedAt(),
				user.lastLoginAt(),
				user.roles().stream().map(Role::code).sorted().toList()))
			.toList();
	}

	/** Retourne les rôles et les permissions qui leur sont associées. */
	@PreAuthorize("hasAuthority('can_platform_role_read')")
	@GetMapping("/role")
	public List<RoleResponse> getRoles(Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return securityDirectoryRepository.findAllRoles().stream()
			.sorted(Comparator.comparing(Role::code))
			.map(role -> new RoleResponse(
				role.code(),
				role.name(),
				role.description(),
				role.enabled(),
				role.permissions().stream()
					.sorted(Comparator.comparing(Permission::code))
					.map(this::toResponse)
					.toList()))
			.toList();
	}

	/** Retourne le catalogue des permissions disponibles. */
	@PreAuthorize("hasAuthority('can_permission_read')")
	@GetMapping("/permission")
	public List<PermissionResponse> getPermissions() {
		return securityDirectoryRepository.findAllPermissions().stream()
			.sorted(Comparator.comparing(Permission::code))
			.map(this::toResponse)
			.toList();
	}

	private PermissionResponse toResponse(Permission permission) {
		return new PermissionResponse(permission.code(), permission.description());
	}
}
