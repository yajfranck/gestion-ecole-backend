package com.galactique.gestionecole.licenses.domain;

import java.time.Instant;
import java.util.UUID;

public record PlatformLicense(Long id, UUID establishmentId, UUID offerId, String offerCode,
	String offerName, Instant validFrom, Instant validUntil, Instant suspendedAt,
	String createdBy, Instant createdAt) {
	public LicenseState stateAt(Instant now) {
		if (validUntil != null && !validUntil.isAfter(now)) {
			return LicenseState.EXPIRED;
		}
		if (suspendedAt != null) {
			return LicenseState.SUSPENDED;
		}
		if (validFrom.isAfter(now)) {
			return LicenseState.UPCOMING;
		}
		return LicenseState.ACTIVE;
	}
}
