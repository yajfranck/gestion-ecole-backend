package com.galactique.gestionecole.security.infrastructure.api;

import java.util.List;

public record RoleResponse(
	String code,
	String name,
	String description,
	boolean enabled,
	List<PermissionResponse> permissions) {
}