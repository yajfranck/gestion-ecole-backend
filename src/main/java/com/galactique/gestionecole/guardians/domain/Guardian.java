package com.galactique.gestionecole.guardians.domain;

import java.time.Instant;
import java.util.UUID;

public record Guardian(UUID id, String firstName, String lastName, String email,
	UUID userAccountId, boolean enabled, Instant createdAt) {
}