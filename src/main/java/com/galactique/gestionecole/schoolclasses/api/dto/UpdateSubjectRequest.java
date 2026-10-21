package com.galactique.gestionecole.schoolclasses.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSubjectRequest(
	@Size(max = 40) String code, @NotBlank @Size(max = 100) String name) {
}
