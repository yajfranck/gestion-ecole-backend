package com.galactique.gestionecole.licenses.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LicenseOffer(UUID id, String code, String name, Set<LicenseFeature> features,
	boolean enabled, Instant createdAt) {
	public LicenseOffer {
		features = Set.copyOf(features);
	}
}
