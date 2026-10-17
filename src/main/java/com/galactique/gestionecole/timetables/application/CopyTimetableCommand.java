package com.galactique.gestionecole.timetables.application;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record CopyTimetableCommand(UUID targetClassroomId, boolean replaceExisting, boolean copyTeachers,
	boolean copyRoomCodes, LocalDate validFrom, LocalDate validUntil, Map<UUID, UUID> teacherMappings) {
	public CopyTimetableCommand { teacherMappings = Map.copyOf(teacherMappings); }
}
