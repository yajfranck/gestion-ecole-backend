package com.galactique.gestionecole.licenses.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.licenses.domain.LicenseState;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;

public record PlatformLicenseResponse(Long id, UUID establishmentId, UUID offerId, String offerCode,
	String offerName, Instant validFrom, Instant validUntil, Instant suspendedAt,
	LicenseState state, String createdBy, Instant createdAt) {
	public static PlatformLicenseResponse from(PlatformLicense license) {
		return new PlatformLicenseResponse(license.id(), license.establishmentId(), license.offerId(),
			license.offerCode(), license.offerName(), license.validFrom(), license.validUntil(),
			license.suspendedAt(), license.stateAt(Instant.now()), license.createdBy(), license.createdAt());
	}
}
