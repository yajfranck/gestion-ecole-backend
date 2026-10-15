package com.galactique.gestionecole.tuition.domain;

import java.time.Instant;
import java.util.UUID;

public record TuitionFeeType(UUID id, UUID establishmentId, String code, String name,
	boolean enabled, Instant createdAt, Instant updatedAt) {
}