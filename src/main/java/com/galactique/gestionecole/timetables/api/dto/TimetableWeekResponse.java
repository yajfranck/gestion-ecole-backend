package com.galactique.gestionecole.timetables.api.dto;

import java.time.LocalDate;
import java.util.List;
import com.galactique.gestionecole.timetables.application.TimetableApplicationService.WeekSchedule;

public record TimetableWeekResponse(LocalDate startsOn, LocalDate endsOn, List<TimetableDayResponse> days) {
	public static TimetableWeekResponse from(WeekSchedule week) {
		return new TimetableWeekResponse(week.startsOn(), week.endsOn(), week.days().stream().map(TimetableDayResponse::from).toList());
	}
}
