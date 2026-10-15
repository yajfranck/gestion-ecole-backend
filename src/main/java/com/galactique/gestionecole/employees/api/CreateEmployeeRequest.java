package com.galactique.gestionecole.employees.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
	@NotBlank @Size(max = 40) String employeeNumber,
	@NotBlank @Size(max = 100) String firstName,
	@NotBlank @Size(max = 100) String lastName,
	@NotBlank @Email @Size(max = 254) String email,
	@NotNull UUID positionId,
	@Size(max = 2) List<@Pattern(regexp = "STAFF|ESTABLISHMENT_ADMIN") String> directRoleCodes) {
}