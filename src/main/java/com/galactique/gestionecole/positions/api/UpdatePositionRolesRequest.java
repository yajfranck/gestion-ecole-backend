package com.galactique.gestionecole.positions.api;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePositionRolesRequest(
	@Size(max = 64) List<@NotBlank @Size(max = 64) String> roleCodes) {
}