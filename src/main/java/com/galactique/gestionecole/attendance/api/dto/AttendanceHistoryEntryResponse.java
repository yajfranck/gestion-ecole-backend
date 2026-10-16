package com.galactique.gestionecole.attendance.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;

public record AttendanceHistoryEntryResponse(LocalDate date, String status, boolean absenceJustified,
	String comment, String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
	public static AttendanceHistoryEntryResponse from(AttendanceRecord attendanceRecord) {
		return new AttendanceHistoryEntryResponse(attendanceRecord.attendanceDate(), attendanceRecord.status().name(),
			attendanceRecord.absenceJustified(), attendanceRecord.comment(), attendanceRecord.createdBy(),
			attendanceRecord.updatedBy(), attendanceRecord.createdAt(), attendanceRecord.updatedAt());
	}
}