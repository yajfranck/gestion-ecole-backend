package com.galactique.gestionecole.schoolclasses.domain;

import java.util.Objects;
import java.util.UUID;

public record ClassroomSubjectTeacher(UUID classroomId, UUID employeeId) {
	public ClassroomSubjectTeacher {
		Objects.requireNonNull(classroomId, "classroomId must not be null");
		Objects.requireNonNull(employeeId, "employeeId must not be null");
	}
}
