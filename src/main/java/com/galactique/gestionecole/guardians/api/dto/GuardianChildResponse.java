package com.galactique.gestionecole.guardians.api.dto;

import java.util.UUID;

import com.galactique.gestionecole.students.domain.Student;

public record GuardianChildResponse(UUID id, UUID establishmentId, String studentNumber,
	String firstName, String lastName) {
	public static GuardianChildResponse from(Student student) {
		return new GuardianChildResponse(student.id(), student.establishmentId(), student.studentNumber(),
			student.firstName(), student.lastName());
	}
}