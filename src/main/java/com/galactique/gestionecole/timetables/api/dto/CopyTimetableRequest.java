package com.galactique.gestionecole.timetables.api.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.galactique.gestionecole.timetables.application.CopyTimetableCommand;

public record CopyTimetableRequest(@NotNull UUID targetClassroomId, Boolean replaceExisting, Boolean copyTeachers,
	Boolean copyRoomCodes, LocalDate validFrom, LocalDate validUntil,
	@Size(max = 100) Map<@NotNull UUID, @NotNull UUID> teacherMappings) {
	public CopyTimetableCommand toCommand() {
		return new CopyTimetableCommand(targetClassroomId, Boolean.TRUE.equals(replaceExisting), Boolean.TRUE.equals(copyTeachers),
			Boolean.TRUE.equals(copyRoomCodes), validFrom, validUntil, teacherMappings == null ? Map.of() : teacherMappings);
	}
}
