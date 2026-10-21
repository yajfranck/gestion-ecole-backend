package com.galactique.gestionecole.security.infrastructure.api.dto;

import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;

public record EstablishmentRoleResponse(UUID establishmentId, String code, String name,
	String description, boolean enabled, boolean editable, List<PermissionResponse> permissions) {
	public static EstablishmentRoleResponse from(Role role) {
		return new EstablishmentRoleResponse(role.establishmentId(), role.code(), role.name(), role.description(),
			role.enabled(), role.establishmentId() != null, role.permissions().stream()
				.sorted(java.util.Comparator.comparing(Permission::code))
				.map(permission -> new PermissionResponse(permission.code(), permission.description()))
				.toList());
	}
}
