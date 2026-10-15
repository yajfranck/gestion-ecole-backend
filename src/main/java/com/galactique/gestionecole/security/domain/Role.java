package com.galactique.gestionecole.security.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public record Role(String code, String name, String description, boolean enabled, Set<Permission> permissions,
	UUID establishmentId) {
	private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

	public Role(String code, String name, String description, boolean enabled, Set<Permission> permissions) {
		this(code, name, description, enabled, permissions, null);
	}

	public Role {
		Objects.requireNonNull(code, "code must not be null");
		if (code.length() > 64 || !CODE_PATTERN.matcher(code).matches()) {
			throw new IllegalArgumentException("Role code must be uppercase and no longer than 64 characters");
		}
		Objects.requireNonNull(name, "name must not be null");
		if (name.isBlank() || name.length() > 100) {
			throw new IllegalArgumentException("Role name must contain between 1 and 100 characters");
		}
		if (description != null && description.length() > 255) {
			throw new IllegalArgumentException("Role description must not exceed 255 characters");
		}
		Objects.requireNonNull(permissions, "permissions must not be null");
		if (permissions.isEmpty()) {
			throw new IllegalArgumentException("A role must have at least one permission");
		}

		Set<String> permissionCodes = new HashSet<>();
		for (Permission permission : permissions) {
			if (!permissionCodes.add(permission.code())) {
				throw new IllegalArgumentException("A role cannot contain duplicate permission codes");
			}
		}
		permissions = Set.copyOf(permissions);
	}
}