package com.galactique.gestionecole.timetables.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

public record TimetableEntry(UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId,
	UUID classroomId, UUID subjectId, UUID teacherId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
	LocalDate validFrom, LocalDate validUntil, String roomCode, boolean enabled, UUID copiedFromId,
	String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
	public boolean occursOn(LocalDate date) {
		return enabled && date.getDayOfWeek() == dayOfWeek && !date.isBefore(validFrom) && !date.isAfter(validUntil);
	}

	/** Adjacent times are permitted; date ranges conflict only if they share an occurrence. */
	public boolean conflictsWith(TimetableEntry other) {
		if (!establishmentId.equals(other.establishmentId) || !enabled || !other.enabled || dayOfWeek != other.dayOfWeek
			|| !startTime.isBefore(other.endTime) || !other.startTime.isBefore(endTime)) { return false; }
		LocalDate from = validFrom.isAfter(other.validFrom) ? validFrom : other.validFrom;
		LocalDate until = validUntil.isBefore(other.validUntil) ? validUntil : other.validUntil;
		if (from.with(TemporalAdjusters.nextOrSame(dayOfWeek)).isAfter(until)) { return false; }
		return classroomId.equals(other.classroomId)
			|| teacherId != null && teacherId.equals(other.teacherId)
			|| roomCode != null && roomCode.equalsIgnoreCase(other.roomCode);
	}
}
