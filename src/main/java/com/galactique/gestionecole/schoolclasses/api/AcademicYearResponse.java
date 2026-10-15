package com.galactique.gestionecole.schoolclasses.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;

public record AcademicYearResponse(UUID id, UUID establishmentId, String name, LocalDate startsOn,
	LocalDate endsOn, boolean enabled, Instant createdAt, Instant updatedAt) {
	public static AcademicYearResponse from(AcademicYear academicYear) {
		return new AcademicYearResponse(academicYear.id(), academicYear.establishmentId(), academicYear.name(),
			academicYear.startsOn(), academicYear.endsOn(), academicYear.enabled(),
			academicYear.createdAt(), academicYear.updatedAt());
	}
}