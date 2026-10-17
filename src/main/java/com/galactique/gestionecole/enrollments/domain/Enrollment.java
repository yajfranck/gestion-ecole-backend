package com.galactique.gestionecole.enrollments.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Enrollment(UUID id, UUID establishmentId, UUID studentId, UUID academicYearId,
	UUID schoolClassId, UUID classroomId, boolean advanceAccepted, String paymentDecisionBy, Instant createdAt) {
	public Enrollment {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		Objects.requireNonNull(studentId, "studentId must not be null");
		Objects.requireNonNull(academicYearId, "academicYearId must not be null");
		Objects.requireNonNull(schoolClassId, "schoolClassId must not be null");
		Objects.requireNonNull(classroomId, "classroomId must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}
}
