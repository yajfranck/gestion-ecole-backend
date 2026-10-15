package com.galactique.gestionecole.establishments.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OnboardEstablishmentRequest(
	@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 200) String name,
	@NotNull @Valid EstablishmentAdministratorRequest administrator) {
}