package com.galactique.gestionecole.timetables.api.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.galactique.gestionecole.timetables.application.TimetableEntryCommand;

public record TimetableEntryRequest(@NotNull UUID subjectId, UUID teacherId, @NotNull DayOfWeek dayOfWeek,
	@NotNull LocalTime startTime, @NotNull LocalTime endTime, LocalDate validFrom, LocalDate validUntil,
	@Size(max = 40) String roomCode) {
	public TimetableEntryCommand toCommand() {
		return new TimetableEntryCommand(subjectId, teacherId, dayOfWeek, startTime, endTime, validFrom, validUntil, roomCode);
	}
}
