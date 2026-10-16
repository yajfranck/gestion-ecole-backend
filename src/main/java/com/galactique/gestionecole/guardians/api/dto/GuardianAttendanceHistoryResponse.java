package com.galactique.gestionecole.guardians.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.attendance.application.AttendanceHistory;

public record GuardianAttendanceHistoryResponse(UUID establishmentId, UUID academicYearId, UUID studentId,
	LocalDate from, LocalDate to, List<GuardianAttendanceEntryResponse> records) {
	public static GuardianAttendanceHistoryResponse from(AttendanceHistory history) {
		return new GuardianAttendanceHistoryResponse(history.establishmentId(), history.academicYearId(),
			history.studentId(), history.from(), history.to(),
			history.records().stream().map(GuardianAttendanceEntryResponse::from).toList());
	}
}
