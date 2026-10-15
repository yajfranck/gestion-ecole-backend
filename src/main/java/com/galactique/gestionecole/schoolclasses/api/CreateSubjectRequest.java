package com.galactique.gestionecole.schoolclasses.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
	@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 100) String name) {
}