package com.galactique.gestionecole.guardians.domain;

import java.time.Instant;
import java.util.UUID;

public record GuardianInvitation(UUID id, UUID guardianId, String tokenHash,
	Instant createdAt, Instant expiresAt, Instant acceptedAt) {
	public boolean isAccepted() {
		return acceptedAt != null;
	}
}