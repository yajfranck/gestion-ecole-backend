package com.galactique.gestionecole.tuition.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentMethodInputRequest(
	@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 100) String name) {
}