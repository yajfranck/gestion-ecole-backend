package com.galactique.gestionecole.schoolclasses.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Classroom(UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId,
	String code, String name, boolean enabled, Instant createdAt, Instant updatedAt) {
	public Classroom {
		Objects.requireNonNull(id);
		Objects.requireNonNull(establishmentId);
		Objects.requireNonNull(academicYearId);
		Objects.requireNonNull(schoolClassId);
		code = required(code, 40).toUpperCase(Locale.ROOT);
		name = required(name, 100);
		Objects.requireNonNull(createdAt);
		Objects.requireNonNull(updatedAt);
	}

	private static String required(String value, int limit) {
		if (value == null || value.isBlank() || value.strip().length() > limit) {
			throw new IllegalArgumentException("Invalid classroom code or name");
		}
		return value.strip();
	}
}
