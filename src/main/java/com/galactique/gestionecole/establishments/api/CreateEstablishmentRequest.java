package com.galactique.gestionecole.establishments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEstablishmentRequest(
	@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 200) String name) {
}