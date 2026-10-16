package com.galactique.gestionecole.attendance.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;

public record AttendanceHistory(UUID establishmentId, UUID academicYearId, UUID studentId,
	LocalDate from, LocalDate to, List<AttendanceRecord> records) {
	public AttendanceHistory {
		records = List.copyOf(records);
	}
}