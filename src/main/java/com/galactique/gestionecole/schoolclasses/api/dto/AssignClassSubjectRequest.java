package com.galactique.gestionecole.schoolclasses.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AssignClassSubjectRequest(List<@NotNull @Valid ClassroomTeacherRequest> classroomTeachers) {
	public record ClassroomTeacherRequest(@NotNull UUID classroomId, @NotNull UUID employeeId) { }
}
