package com.galactique.gestionecole.enrollments.application;

import java.util.UUID;

public record CreateEnrollmentCommand(UUID studentId, UUID schoolClassId) {
}