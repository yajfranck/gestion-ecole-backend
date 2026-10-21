package com.galactique.gestionecole.students.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
	@Size(min = 1, max = 100) String firstName,
	@Size(min = 1, max = 100) String lastName,
	@PastOrPresent LocalDate dateOfBirth) {
	public boolean hasChanges() {
		return firstName != null || lastName != null || dateOfBirth != null;
	}
}