package com.galactique.gestionecole.licenses.domain;

import java.time.Instant;
public record LicenseEvent(Long id, Long licenseId, String action, String actor,
	String details, Instant createdAt) {
}
