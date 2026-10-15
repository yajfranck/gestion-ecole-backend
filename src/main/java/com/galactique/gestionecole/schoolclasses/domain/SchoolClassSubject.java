package com.galactique.gestionecole.schoolclasses.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SchoolClassSubject(UUID id, UUID establishmentId, UUID schoolClassId, UUID subjectId,
	boolean enabled, Instant createdAt, Instant updatedAt) {
	public SchoolClassSubject {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		Objects.requireNonNull(schoolClassId, "schoolClassId must not be null");
		Objects.requireNonNull(subjectId, "subjectId must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}