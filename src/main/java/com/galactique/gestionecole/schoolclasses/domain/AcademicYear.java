package com.galactique.gestionecole.schoolclasses.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record AcademicYear(UUID id, UUID establishmentId, String name, LocalDate startsOn, LocalDate endsOn,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public AcademicYear {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		name = required(name, "name");
		Objects.requireNonNull(startsOn, "startsOn must not be null");
		Objects.requireNonNull(endsOn, "endsOn must not be null");
		if (endsOn.isBefore(startsOn)) {
			throw new IllegalArgumentException("Academic year end must not be before its start");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	private static String required(String value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isBlank() || normalized.length() > 100) {
			throw new IllegalArgumentException(field + " must contain between 1 and 100 characters");
		}
		return normalized;
	}
}