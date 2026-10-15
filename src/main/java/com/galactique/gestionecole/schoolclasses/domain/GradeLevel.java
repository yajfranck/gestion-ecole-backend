package com.galactique.gestionecole.schoolclasses.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record GradeLevel(UUID id, UUID establishmentId, String code, String name, int displayOrder,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public GradeLevel {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		code = required(code, "code", 40).toUpperCase(Locale.ROOT);
		name = required(name, "name", 100);
		if (displayOrder < 0) {
			throw new IllegalArgumentException("displayOrder must be zero or greater");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	private static String required(String value, String field, int maxLength) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isBlank() || normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must contain between 1 and " + maxLength + " characters");
		}
		return normalized;
	}
}