package com.galactique.gestionecole.attendance.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.attendance.application.AttendanceSheet;

public record AttendanceSheetResponse(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
	LocalDate date, int presentCount, int absentCount, int unreportedCount, List<AttendanceEntryResponse> entries) {
	public static AttendanceSheetResponse from(AttendanceSheet sheet) {
		return new AttendanceSheetResponse(sheet.establishmentId(), sheet.academicYearId(), sheet.schoolClassId(),
			sheet.classroomId(), sheet.date(), sheet.presentCount(), sheet.absentCount(), sheet.unreportedCount(),
			sheet.entries().stream().map(AttendanceEntryResponse::from).toList());
	}
}
