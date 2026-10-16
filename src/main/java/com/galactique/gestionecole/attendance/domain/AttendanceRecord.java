package com.galactique.gestionecole.attendance.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record AttendanceRecord(UUID id, UUID enrollmentId, LocalDate attendanceDate,
	AttendanceStatus status, boolean absenceJustified, String comment, String parentComment,
	String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
	public AttendanceRecord {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
		Objects.requireNonNull(attendanceDate, "attendanceDate must not be null");
		Objects.requireNonNull(status, "status must not be null");
		if (absenceJustified && status != AttendanceStatus.ABSENT) {
			throw new IllegalArgumentException("Only an absence can be justified");
		}
		if (comment != null) {
			comment = comment.strip();
			if (comment.isEmpty()) {
				comment = null;
			} else if (comment.length() > 500) {
				throw new IllegalArgumentException("Comment must not exceed 500 characters");
			}
		}
		parentComment = optionalComment(parentComment);
		createdBy = required(createdBy, "createdBy");
		updatedBy = required(updatedBy, "updatedBy");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	private static String required(String value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isEmpty() || normalized.length() > 100) {
			throw new IllegalArgumentException(field + " must contain between 1 and 100 characters");
		}
		return normalized;
	}

	private static String optionalComment(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.strip();
		if (normalized.length() > 500) {
			throw new IllegalArgumentException("Parent comment must not exceed 500 characters");
		}
		return normalized;
	}
}