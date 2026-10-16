package com.galactique.gestionecole.attendance.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.attendance.application.AttendanceHistory;

public record AttendanceHistoryResponse(UUID establishmentId, UUID academicYearId, UUID studentId,
	LocalDate from, LocalDate to, List<AttendanceHistoryEntryResponse> records) {
	public static AttendanceHistoryResponse from(AttendanceHistory history) {
		return new AttendanceHistoryResponse(history.establishmentId(), history.academicYearId(), history.studentId(),
			history.from(), history.to(), history.records().stream().map(AttendanceHistoryEntryResponse::from).toList());
	}
}