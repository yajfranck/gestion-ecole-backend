package com.galactique.gestionecole.schoolclasses.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.schoolclasses.domain.AcademicPeriod;

public record AcademicPeriodResponse(UUID id, UUID academicYearId, String name, LocalDate startsOn,
	LocalDate endsOn, boolean enabled, Instant createdAt, Instant updatedAt) {
	public static AcademicPeriodResponse from(AcademicPeriod period) {
		return new AcademicPeriodResponse(period.id(), period.academicYearId(), period.name(),
			period.startsOn(), period.endsOn(), period.enabled(), period.createdAt(), period.updatedAt());
	}
}