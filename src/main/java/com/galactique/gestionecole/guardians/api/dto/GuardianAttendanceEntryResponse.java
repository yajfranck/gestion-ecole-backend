package com.galactique.gestionecole.guardians.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;

public record GuardianAttendanceEntryResponse(LocalDate date, String status, boolean absenceJustified,
	String parentComment, Instant createdAt, Instant updatedAt) {
	public static GuardianAttendanceEntryResponse from(AttendanceRecord attendanceRecord) {
		return new GuardianAttendanceEntryResponse(attendanceRecord.attendanceDate(),
			attendanceRecord.status().name(), attendanceRecord.absenceJustified(),
			attendanceRecord.parentComment(), attendanceRecord.createdAt(), attendanceRecord.updatedAt());
	}
}
