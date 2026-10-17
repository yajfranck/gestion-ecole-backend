package com.galactique.gestionecole.schoolclasses.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassRequest(
	@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 100) String name,
	@Size(max = 100) String series) {
}
