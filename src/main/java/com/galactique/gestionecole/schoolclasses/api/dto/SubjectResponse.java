package com.galactique.gestionecole.schoolclasses.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.schoolclasses.domain.Subject;

public record SubjectResponse(UUID id, UUID establishmentId, String code, String name,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public static SubjectResponse from(Subject subject) {
		return new SubjectResponse(subject.id(), subject.establishmentId(), subject.code(), subject.name(),
			subject.enabled(), subject.createdAt(), subject.updatedAt());
	}
}
