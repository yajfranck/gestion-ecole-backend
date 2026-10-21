package com.galactique.gestionecole.security.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
	@NotBlank @Size(max = 100) String login,
	@NotBlank String password) {
}
