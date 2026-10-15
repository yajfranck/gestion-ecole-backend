package com.galactique.gestionecole.enrollments.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentRequest(@NotNull UUID studentId, @NotNull UUID schoolClassId) {
}