package com.galactique.gestionecole.timetables.api.dto;

import java.time.LocalDate;
import com.galactique.gestionecole.timetables.domain.NonWorkingDay;

public record NonWorkingDayResponse(LocalDate date, String name) {
	public static NonWorkingDayResponse from(NonWorkingDay day) { return new NonWorkingDayResponse(day.date(), day.name()); }
}
