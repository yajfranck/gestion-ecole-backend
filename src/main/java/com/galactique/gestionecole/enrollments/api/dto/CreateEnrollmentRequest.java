package com.galactique.gestionecole.enrollments.api.dto;

import java.util.UUID;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentRequest(@NotNull UUID studentId, @NotNull UUID schoolClassId, UUID classroomId,
	Boolean acceptAdvance, @Size(max = 100) List<@NotNull @Valid EnrollmentPaymentRequest> payments) {
}
