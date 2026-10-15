package com.galactique.gestionecole.security.infrastructure.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
	@NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*") String code,
	@NotBlank @Size(max = 100) String name,
	@Size(max = 255) String description,
	@NotEmpty List<@NotBlank @Size(max = 64) String> permissionCodes) {
}