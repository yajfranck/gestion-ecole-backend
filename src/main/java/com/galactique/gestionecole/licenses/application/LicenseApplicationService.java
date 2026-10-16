package com.galactique.gestionecole.licenses.application;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.establishments.application.EstablishmentApplicationService;
import com.galactique.gestionecole.establishments.application.EstablishmentNotFoundException;
import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseEvent;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseOffer;
import com.galactique.gestionecole.licenses.domain.LicenseRepository;
import com.galactique.gestionecole.licenses.domain.LicenseState;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;

@Service
public class LicenseApplicationService {
	private final LicenseRepository licenseRepository;
	private final EstablishmentApplicationService establishmentApplicationService;
	private final EmployeeApplicationService employeeApplicationService;

	public LicenseApplicationService(LicenseRepository licenseRepository,
		EstablishmentApplicationService establishmentApplicationService,
		EmployeeApplicationService employeeApplicationService) {
		this.licenseRepository = licenseRepository;
		this.establishmentApplicationService = establishmentApplicationService;
		this.employeeApplicationService = employeeApplicationService;
	}

	@Transactional(readOnly = true)
	public List<LicenseOffer> findOffers() {
		return licenseRepository.findOffers();
	}

	@Transactional
	public LicenseOffer createOffer(String code, String name, Set<LicenseFeature> features) {
		if (code == null || !code.strip().matches("[A-Za-z0-9_-]{2,40}")
			|| name == null || name.isBlank() || name.strip().length() > 100
			|| features == null || features.isEmpty()) {
			throw new LicenseValidationException();
		}
		try {
			return licenseRepository.createOffer(code.strip().toUpperCase(java.util.Locale.ROOT),
				name.strip(), features);
		} catch (DataIntegrityViolationException exception) {
			throw new LicenseConflictException();
		}
	}

	@Transactional(readOnly = true)
	public List<PlatformLicense> findLicenses(UUID establishmentId, EmployeeActor actor) {
		try {
			boolean accessible = employeeApplicationService.findEstablishments(actor).stream()
				.anyMatch(establishment -> establishment.enabled() && establishment.id().equals(establishmentId));
			if (!accessible) {
				throw new LicenseNotFoundException();
			}
		} catch (EmployeeAccessDeniedException exception) {
			throw new LicenseAccessDeniedException();
		}
		return licenseRepository.findLicenses(establishmentId);
	}

	@Transactional(readOnly = true)
	public List<LicenseEvent> findEvents(Long licenseId) {
		return licenseRepository.findEvents(licenseId);
	}

	@Transactional
	public PlatformLicense assign(UUID establishmentId, UUID offerId, Instant validFrom,
		Instant validUntil, String actor) {
		establishmentApplicationService.lockForLicenseManagement(establishmentId);
		Instant now = Instant.now();
		Instant start = validFrom == null ? now : validFrom;
		if (start.isBefore(now) || (validUntil != null && !validUntil.isAfter(start))) {
			throw new LicenseValidationException();
		}
		LicenseOffer offer = licenseRepository.findEnabledOffer(offerId)
			.orElseThrow(LicenseNotFoundException::new);
		List<PlatformLicense> licenses = licenseRepository.findLicensesForUpdate(establishmentId);
		for (PlatformLicense existing : licenses) {
			if (!overlaps(existing.validFrom(), existing.validUntil(), start, validUntil)) {
				continue;
			}
			if (!existing.validFrom().isBefore(start)) {
				throw new LicenseConflictException();
			}
			licenseRepository.endLicense(existing, start, actor,
				"REPLACED", "License period ended by a manual replacement");
		}
		PlatformLicense created;
		try {
			created = licenseRepository.createLicense(establishmentId, offer, start, validUntil, actor);
		} catch (DataIntegrityViolationException exception) {
			throw new LicenseConflictException();
		}
		licenseRepository.recordEvent(created, "ASSIGNED", actor, "Offer " + offer.code() + " assigned");
		return created;
	}

	@Transactional
	public PlatformLicense suspend(UUID establishmentId, Long licenseId, String actor) {
		PlatformLicense license = lockLicense(establishmentId, licenseId);
		if (license.stateAt(Instant.now()) == LicenseState.SUSPENDED
			|| license.stateAt(Instant.now()) == LicenseState.EXPIRED) {
			throw new LicenseConflictException();
		}
		return licenseRepository.suspendLicense(license, actor);
	}

	@Transactional
	public PlatformLicense reactivate(UUID establishmentId, Long licenseId, String actor) {
		PlatformLicense license = lockLicense(establishmentId, licenseId);
		if (license.stateAt(Instant.now()) != LicenseState.SUSPENDED
			|| (license.validUntil() != null && !license.validUntil().isAfter(Instant.now()))) {
			throw new LicenseConflictException();
		}
		return licenseRepository.reactivateLicense(license, actor);
	}

	@Transactional
	public PlatformLicense end(UUID establishmentId, Long licenseId, String actor) {
		PlatformLicense license = lockLicense(establishmentId, licenseId);
		Instant now = Instant.now();
		if (license.validFrom().isAfter(now) || license.stateAt(now) == LicenseState.EXPIRED) {
			throw new LicenseConflictException();
		}
		return licenseRepository.endLicense(license, now, actor,
			"ENDED", "License ended by platform administrator");
	}

	@Transactional(readOnly = true)
	public boolean hasFeature(UUID establishmentId, LicenseFeature feature, Instant at) {
		return licenseRepository.findLicenses(establishmentId).stream()
			.filter(license -> license.stateAt(at) == LicenseState.ACTIVE)
			.anyMatch(license -> licenseRepository.findEnabledOffer(license.offerId())
				.map(offer -> offer.features().contains(feature)).orElse(false));
	}

	private PlatformLicense lockLicense(UUID establishmentId, Long licenseId) {
		establishmentApplicationService.lockForLicenseManagement(establishmentId);
		return licenseRepository.findLicensesForUpdate(establishmentId).stream()
			.filter(license -> license.id().equals(licenseId))
			.findFirst().orElseThrow(LicenseNotFoundException::new);
	}

	private boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
		return (firstEnd == null || secondStart.isBefore(firstEnd))
			&& (secondEnd == null || firstStart.isBefore(secondEnd));
	}
}
