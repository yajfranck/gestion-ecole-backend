package com.galactique.gestionecole.licenses.api.dto;

import java.time.Instant;

import com.galactique.gestionecole.licenses.domain.LicenseEvent;

public record LicenseEventResponse(Long id, Long licenseId, String action, String actor,
	String details, Instant createdAt) {
	public static LicenseEventResponse from(LicenseEvent event) {
		return new LicenseEventResponse(event.id(), event.licenseId(), event.action(), event.actor(),
			event.details(), event.createdAt());
	}
}
