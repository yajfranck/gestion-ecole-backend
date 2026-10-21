package com.galactique.gestionecole.students.application;

import java.time.LocalDate;

public record UpdateStudentCommand(String firstName, String lastName, LocalDate dateOfBirth) {
	public UpdateStudentCommand(String firstName, String lastName) { this(firstName, lastName, null); }
}
