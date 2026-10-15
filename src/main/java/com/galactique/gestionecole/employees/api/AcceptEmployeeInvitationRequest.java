package com.galactique.gestionecole.employees.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptEmployeeInvitationRequest(
	@NotBlank @Size(max = 100) String token,
	@NotBlank @Size(min = 12, max = 72) String password) {
}