package com.galactique.gestionecole.students.application;

import java.time.LocalDate;

public record CreateStudentCommand(String firstName, String lastName, LocalDate dateOfBirth) {
	public CreateStudentCommand(String firstName, String lastName) { this(firstName, lastName, null); }
}
