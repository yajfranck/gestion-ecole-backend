package com.galactique.gestionecole.attendance.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.attendance.domain.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendanceEntryRequest(@NotNull UUID studentId, @NotNull AttendanceStatus status,
	Boolean absenceJustified, @Size(max = 500) String comment,
	@Size(max = 500) String parentComment, Instant expectedUpdatedAt) {
}