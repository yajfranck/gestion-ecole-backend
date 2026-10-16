package com.galactique.gestionecole.attendance.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.attendance.application.AttendanceSheetEntry;
import com.galactique.gestionecole.attendance.domain.AttendanceRecord;

public record AttendanceEntryResponse(UUID studentId, String studentNumber, String firstName, String lastName,
	String status, boolean absenceJustified, String comment, String parentComment, String createdBy, String updatedBy,
	Instant createdAt, Instant updatedAt) {
	public static AttendanceEntryResponse from(AttendanceSheetEntry entry) {
		AttendanceRecord attendanceRecord = entry.attendanceRecord();
		return new AttendanceEntryResponse(entry.student().id(), entry.student().studentNumber(),
			entry.student().firstName(), entry.student().lastName(),
			attendanceRecord == null ? "NON_RENSEIGNE" : attendanceRecord.status().name(),
			attendanceRecord != null && attendanceRecord.absenceJustified(),
			attendanceRecord == null ? null : attendanceRecord.comment(),
			attendanceRecord == null ? null : attendanceRecord.parentComment(),
			attendanceRecord == null ? null : attendanceRecord.createdBy(),
			attendanceRecord == null ? null : attendanceRecord.updatedBy(),
			attendanceRecord == null ? null : attendanceRecord.createdAt(),
			attendanceRecord == null ? null : attendanceRecord.updatedAt());
	}
}