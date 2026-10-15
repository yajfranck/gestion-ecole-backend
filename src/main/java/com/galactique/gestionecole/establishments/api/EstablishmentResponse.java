package com.galactique.gestionecole.establishments.api;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.establishments.domain.Establishment;

public record EstablishmentResponse(UUID id, String code, String name, boolean enabled, Instant createdAt,
	Instant updatedAt) {
	public static EstablishmentResponse from(Establishment establishment) {
		return new EstablishmentResponse(establishment.id(), establishment.code(), establishment.name(),
			establishment.enabled(), establishment.createdAt(), establishment.updatedAt());
	}
}