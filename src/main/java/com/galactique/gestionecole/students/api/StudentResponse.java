package com.galactique.gestionecole.students.api;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.students.domain.Student;

public record StudentResponse(UUID id, UUID establishmentId, String studentNumber, String firstName,
	String lastName, boolean enabled, Instant createdAt, Instant updatedAt) {
	public static StudentResponse from(Student student) {
		return new StudentResponse(student.id(), student.establishmentId(), student.studentNumber(),
			student.firstName(), student.lastName(), student.enabled(), student.createdAt(), student.updatedAt());
	}
}