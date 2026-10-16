package com.galactique.gestionecole.schoolclasses.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record SchoolClass(UUID id, UUID establishmentId, UUID academicYearId, String code, String name,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public SchoolClass {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		Objects.requireNonNull(academicYearId, "academicYearId must not be null");
		code = required(code, "code", 40).toUpperCase(Locale.ROOT);
		name = required(name, "name", 100);
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