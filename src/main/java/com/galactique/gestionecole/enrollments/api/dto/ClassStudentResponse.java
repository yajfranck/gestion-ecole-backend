package com.galactique.gestionecole.enrollments.api.dto;

import java.util.UUID;

import com.galactique.gestionecole.students.domain.Student;

public record ClassStudentResponse(UUID id, String studentNumber, String firstName, String lastName) {
	public static ClassStudentResponse from(Student student) {
		return new ClassStudentResponse(student.id(), student.studentNumber(), student.firstName(), student.lastName());
	}
}
