package com.galactique.gestionecole.security.infrastructure.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
	@NotBlank @Size(max = 100) String name,
	@Size(max = 255) String description,
	@NotNull List<@NotBlank @Size(max = 64) String> permissionCodes) {
}
