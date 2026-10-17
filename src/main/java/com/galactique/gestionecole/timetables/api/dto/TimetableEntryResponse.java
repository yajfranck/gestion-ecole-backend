package com.galactique.gestionecole.timetables.api.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import com.galactique.gestionecole.timetables.domain.TimetableEntry;

public record TimetableEntryResponse(UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId,
	UUID classroomId, UUID subjectId, UUID teacherId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
	LocalDate validFrom, LocalDate validUntil, String roomCode, boolean enabled, UUID copiedFromId,
	String createdBy, String updatedBy, Instant createdAt, Instant updatedAt) {
	public static TimetableEntryResponse from(TimetableEntry e) {
		return new TimetableEntryResponse(e.id(), e.establishmentId(), e.academicYearId(), e.schoolClassId(), e.classroomId(),
			e.subjectId(), e.teacherId(), e.dayOfWeek(), e.startTime(), e.endTime(), e.validFrom(), e.validUntil(), e.roomCode(),
			e.enabled(), e.copiedFromId(), e.createdBy(), e.updatedBy(), e.createdAt(), e.updatedAt());
	}
}
