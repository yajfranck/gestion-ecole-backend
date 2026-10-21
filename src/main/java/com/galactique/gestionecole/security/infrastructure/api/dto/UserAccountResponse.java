package com.galactique.gestionecole.security.infrastructure.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserAccountResponse(
	UUID id,
	String username,
	boolean enabled,
	boolean locked,
	Instant createdAt,
	Instant updatedAt,
	Instant lastLoginAt,
	List<String> roles) {
}
