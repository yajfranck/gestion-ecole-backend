package com.galactique.gestionecole.students.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
	@NotBlank @Size(max = 100) String firstName,
	@NotBlank @Size(max = 100) String lastName) {
}