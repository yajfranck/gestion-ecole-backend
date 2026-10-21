package com.galactique.gestionecole.positions.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
	@NotBlank @Size(max = 100) String name,
	@Size(max = 64) List<@NotBlank @Size(max = 64) String> roleCodes) {
}
