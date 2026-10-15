package com.galactique.gestionecole.positions.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Position(UUID id, UUID establishmentId, String name, boolean enabled,
	Set<String> roleCodes, Instant createdAt, Instant updatedAt) {
	public Position {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		Objects.requireNonNull(name, "name must not be null");
		name = name.strip();
		if (name.isBlank() || name.length() > 100) {
			throw new IllegalArgumentException("Position name must contain between 1 and 100 characters");
		}
		Objects.requireNonNull(roleCodes, "roleCodes must not be null");
		roleCodes = roleCodes.stream().map(role -> role.strip().toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}