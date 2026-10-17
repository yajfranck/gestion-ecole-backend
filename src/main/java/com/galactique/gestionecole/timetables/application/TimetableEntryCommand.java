package com.galactique.gestionecole.timetables.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TimetableEntryCommand(UUID subjectId, UUID teacherId, DayOfWeek dayOfWeek,
	LocalTime startTime, LocalTime endTime, LocalDate validFrom, LocalDate validUntil, String roomCode) { }
