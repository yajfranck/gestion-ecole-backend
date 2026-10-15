package com.galactique.gestionecole.schoolclasses.api;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.schoolclasses.domain.GradeLevel;

public record GradeLevelResponse(UUID id, UUID establishmentId, String code, String name, int displayOrder,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public static GradeLevelResponse from(GradeLevel gradeLevel) {
		return new GradeLevelResponse(gradeLevel.id(), gradeLevel.establishmentId(), gradeLevel.code(),
			gradeLevel.name(), gradeLevel.displayOrder(), gradeLevel.enabled(),
			gradeLevel.createdAt(), gradeLevel.updatedAt());
	}
}