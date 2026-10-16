package com.galactique.gestionecole.attendance.application;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.attendance.domain.AttendanceStatus;

public record AttendanceEntryCommand(UUID studentId, AttendanceStatus status,
	boolean absenceJustified, String comment, String parentComment, Instant expectedUpdatedAt) {
}