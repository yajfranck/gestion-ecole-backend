package com.galactique.gestionecole.enrollments.api.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record ChangeEnrollmentClassroomRequest(@NotNull UUID classroomId) { }
