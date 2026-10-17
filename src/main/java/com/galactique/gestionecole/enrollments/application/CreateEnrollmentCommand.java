package com.galactique.gestionecole.enrollments.application;

import java.util.UUID;
import java.util.List;

public record CreateEnrollmentCommand(UUID studentId, UUID schoolClassId, UUID classroomId,
	boolean acceptAdvance, List<EnrollmentPaymentInput> payments) {
	public CreateEnrollmentCommand { payments = payments == null ? List.of() : List.copyOf(payments); }
}
