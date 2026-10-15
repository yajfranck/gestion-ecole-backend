package com.galactique.gestionecole.security.infrastructure.api.dto;

import java.util.Comparator;
import java.util.List;

import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;

public record ManagedRoleResponse(String code, String name, String description,
	boolean enabled, List<ManagedPermissionResponse> permissions) {
	public static ManagedRoleResponse from(Role role) {
		return new ManagedRoleResponse(role.code(), role.name(), role.description(), role.enabled(),
			role.permissions().stream().sorted(Comparator.comparing(Permission::code))
				.map(permission -> new ManagedPermissionResponse(permission.code(), permission.description())).toList());
	}
}