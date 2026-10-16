package com.galactique.gestionecole.licenses.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseOffer;

public record LicenseOfferResponse(UUID id, String code, String name, Set<LicenseFeature> features,
	boolean enabled, Instant createdAt) {
	public static LicenseOfferResponse from(LicenseOffer offer) {
		return new LicenseOfferResponse(offer.id(), offer.code(), offer.name(), offer.features(),
			offer.enabled(), offer.createdAt());
	}
}
