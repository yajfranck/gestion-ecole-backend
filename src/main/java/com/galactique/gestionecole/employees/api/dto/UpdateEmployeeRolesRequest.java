package com.galactique.gestionecole.employees.api.dto;

import java.util.List;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRolesRequest(
	@Size(max = 2) List<@Pattern(regexp = "STAFF|ESTABLISHMENT_ADMIN") String> roleCodes) {
}
