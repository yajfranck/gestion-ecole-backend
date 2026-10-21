package com.galactique.gestionecole.employees.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
	@Size(min = 1, max = 40) @Pattern(regexp = ".*\\S.*") String employeeNumber,
	@Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*") String firstName,
	@Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*") String lastName,
	@Email @Size(min = 1, max = 254) @Pattern(regexp = ".*\\S.*") String email,
	UUID positionId) {
	public boolean hasChanges() {
		return employeeNumber != null || firstName != null || lastName != null || email != null || positionId != null;
	}
}
