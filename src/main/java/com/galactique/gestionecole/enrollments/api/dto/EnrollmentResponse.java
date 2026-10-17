package com.galactique.gestionecole.enrollments.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.enrollments.domain.Enrollment;

public record EnrollmentResponse(UUID id, UUID establishmentId, UUID studentId, UUID academicYearId,
	UUID schoolClassId, UUID classroomId, boolean advanceAccepted, String paymentDecisionBy, Instant createdAt) {
	public static EnrollmentResponse from(Enrollment enrollment) {
		return new EnrollmentResponse(enrollment.id(), enrollment.establishmentId(), enrollment.studentId(),
			enrollment.academicYearId(), enrollment.schoolClassId(), enrollment.classroomId(),
			enrollment.advanceAccepted(), enrollment.paymentDecisionBy(), enrollment.createdAt());
	}
}
