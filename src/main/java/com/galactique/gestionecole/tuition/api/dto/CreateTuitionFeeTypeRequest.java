package com.galactique.gestionecole.tuition.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTuitionFeeTypeRequest(
	@NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*") String code,
	@NotBlank @Size(max = 100) String name) {
}