package com.galactique.gestionecole.attendance.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AttendanceSheet(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
	LocalDate date, int presentCount, int absentCount, int unreportedCount, List<AttendanceSheetEntry> entries) {
	public AttendanceSheet {
		entries = List.copyOf(entries);
	}
}
