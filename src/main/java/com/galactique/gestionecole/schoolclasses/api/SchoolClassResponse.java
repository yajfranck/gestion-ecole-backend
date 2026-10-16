package com.galactique.gestionecole.schoolclasses.api;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;

public record SchoolClassResponse(UUID id, UUID establishmentId, UUID academicYearId, String code, String name,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public static SchoolClassResponse from(SchoolClass schoolClass) {
		return new SchoolClassResponse(schoolClass.id(), schoolClass.establishmentId(), schoolClass.academicYearId(),
			schoolClass.code(), schoolClass.name(), schoolClass.enabled(),
			schoolClass.createdAt(), schoolClass.updatedAt());
	}
}