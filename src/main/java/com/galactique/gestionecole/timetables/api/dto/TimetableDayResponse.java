package com.galactique.gestionecole.timetables.api.dto;

import java.time.LocalDate;
import java.util.List;
import com.galactique.gestionecole.timetables.application.TimetableApplicationService.DaySchedule;

public record TimetableDayResponse(LocalDate date, boolean withinAcademicYear, String nonWorkingDay,
	List<TimetableEntryResponse> entries) {
	public static TimetableDayResponse from(DaySchedule day) {
		return new TimetableDayResponse(day.date(), day.withinAcademicYear(), day.nonWorkingDay(),
			day.entries().stream().map(TimetableEntryResponse::from).toList());
	}
}
