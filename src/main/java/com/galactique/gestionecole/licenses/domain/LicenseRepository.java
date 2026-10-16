package com.galactique.gestionecole.licenses.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LicenseRepository {
	List<LicenseOffer> findOffers();

	Optional<LicenseOffer> findEnabledOffer(UUID offerId);

	LicenseOffer createOffer(String code, String name, Set<LicenseFeature> features);

	List<PlatformLicense> findLicenses(UUID establishmentId);

	List<PlatformLicense> findLicensesForUpdate(UUID establishmentId);

	PlatformLicense createLicense(UUID establishmentId, LicenseOffer offer, Instant from, Instant until, String actor);

	PlatformLicense endLicense(PlatformLicense license, Instant validUntil, String actor,
		String action, String details);

	PlatformLicense suspendLicense(PlatformLicense license, String actor);

	PlatformLicense reactivateLicense(PlatformLicense license, String actor);

	void recordEvent(PlatformLicense license, String action, String actor, String details);

	List<LicenseEvent> findEvents(Long licenseId);
}
