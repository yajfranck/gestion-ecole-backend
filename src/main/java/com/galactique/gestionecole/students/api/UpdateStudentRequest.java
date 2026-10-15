package com.galactique.gestionecole.students.api;

import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
	@Size(min = 1, max = 100) String firstName,
	@Size(min = 1, max = 100) String lastName) {
	public boolean hasChanges() {
		return firstName != null || lastName != null;
	}
}