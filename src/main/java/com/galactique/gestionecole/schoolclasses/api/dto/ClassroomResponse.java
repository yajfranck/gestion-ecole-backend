package com.galactique.gestionecole.schoolclasses.api.dto;

import java.time.Instant;
import java.util.UUID;
import com.galactique.gestionecole.schoolclasses.domain.Classroom;

public record ClassroomResponse(UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId,
	String code, String name, boolean enabled, Instant createdAt, Instant updatedAt) {
	public static ClassroomResponse from(Classroom classroom) {
		return new ClassroomResponse(classroom.id(), classroom.establishmentId(), classroom.academicYearId(),
			classroom.schoolClassId(), classroom.code(), classroom.name(), classroom.enabled(),
			classroom.createdAt(), classroom.updatedAt());
	}
}
