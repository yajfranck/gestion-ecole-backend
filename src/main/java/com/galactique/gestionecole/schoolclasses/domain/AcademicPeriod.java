package com.galactique.gestionecole.schoolclasses.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record AcademicPeriod(UUID id, UUID academicYearId, String name, LocalDate startsOn, LocalDate endsOn,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public AcademicPeriod {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(academicYearId, "academicYearId must not be null");
		Objects.requireNonNull(name, "name must not be null");
		name = name.strip();
		if (name.isBlank() || name.length() > 100) {
			throw new IllegalArgumentException("Period name must contain between 1 and 100 characters");
		}
		Objects.requireNonNull(startsOn, "startsOn must not be null");
		Objects.requireNonNull(endsOn, "endsOn must not be null");
		if (endsOn.isBefore(startsOn)) {
			throw new IllegalArgumentException("Academic period end must not be before its start");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}