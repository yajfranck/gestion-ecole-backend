package com.galactique.gestionecole.licenses.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.establishments.application.EstablishmentApplicationService;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseOffer;
import com.galactique.gestionecole.licenses.domain.LicenseRepository;
import com.galactique.gestionecole.licenses.domain.LicenseState;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;

@ExtendWith(MockitoExtension.class)
class LicenseApplicationServiceTests {
	private static final UUID ESTABLISHMENT_ID = UUID.randomUUID();
	private static final UUID OFFER_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.now();

	@Mock
	private LicenseRepository licenseRepository;
	@Mock
	private EstablishmentApplicationService establishmentApplicationService;
	@Mock
	private EmployeeApplicationService employeeApplicationService;

	private LicenseApplicationService service;
	private LicenseOffer offer;

	@BeforeEach
	void setUp() {
		service = new LicenseApplicationService(licenseRepository, establishmentApplicationService,
			employeeApplicationService);
		offer = new LicenseOffer(OFFER_ID, "SCHOOL_PLUS", "School Plus",
			Set.of(LicenseFeature.SCHOOL_MANAGEMENT, LicenseFeature.LEARNING_CONTENT), true, NOW);
	}

	@Test
	void assignmentEndsOverlappingLegacyLicenseAtNewStart() {
		Instant start = NOW.plusSeconds(60);
		PlatformLicense legacy = license(1L, NOW.minusSeconds(60), null, LicenseState.ACTIVE);
		PlatformLicense replacement = license(2L, start, null, LicenseState.UPCOMING);
		when(licenseRepository.findEnabledOffer(OFFER_ID)).thenReturn(Optional.of(offer));
		when(licenseRepository.findLicensesForUpdate(ESTABLISHMENT_ID)).thenReturn(List.of(legacy));
		when(licenseRepository.createLicense(eq(ESTABLISHMENT_ID), eq(offer), eq(start), eq(null), eq("admin")))
			.thenReturn(replacement);

		PlatformLicense result = service.assign(ESTABLISHMENT_ID, OFFER_ID, start, null, "admin");

		assertEquals(2L, result.id());
		verify(establishmentApplicationService).lockForLicenseManagement(ESTABLISHMENT_ID);
		verify(licenseRepository).endLicense(legacy, start, "admin",
			"REPLACED", "License period ended by a manual replacement");
		verify(licenseRepository).recordEvent(replacement, "ASSIGNED", "admin", "Offer SCHOOL_PLUS assigned");
	}

	@Test
	void assignmentRejectsOverlappingUpcomingLicense() {
		Instant existingStart = NOW.plusSeconds(120);
		Instant requestedStart = NOW.plusSeconds(60);
		PlatformLicense upcoming = license(3L, existingStart, null, LicenseState.UPCOMING);
		when(licenseRepository.findEnabledOffer(OFFER_ID)).thenReturn(Optional.of(offer));
		when(licenseRepository.findLicensesForUpdate(ESTABLISHMENT_ID)).thenReturn(List.of(upcoming));

		assertThrows(LicenseConflictException.class,
			() -> service.assign(ESTABLISHMENT_ID, OFFER_ID, requestedStart, null, "admin"));
		verify(licenseRepository, never()).createLicense(any(), any(), any(), any(), any());
	}

	@Test
	void suspendedLicenseDoesNotGrantFeature() {
		PlatformLicense suspended = new PlatformLicense(4L, ESTABLISHMENT_ID, OFFER_ID, offer.code(),
			offer.name(), NOW.minusSeconds(60), null, NOW.minusSeconds(5), "admin", NOW.minusSeconds(60));
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of(suspended));

		assertFalse(service.hasFeature(ESTABLISHMENT_ID, LicenseFeature.LEARNING_CONTENT, NOW));
		PlatformLicense expiredSuspended = new PlatformLicense(6L, ESTABLISHMENT_ID, OFFER_ID, offer.code(),
			offer.name(), NOW.minusSeconds(120), NOW.minusSeconds(10), NOW.minusSeconds(30), "admin", NOW);
		assertEquals(LicenseState.EXPIRED, expiredSuspended.stateAt(NOW));
		verify(licenseRepository).findLicenses(ESTABLISHMENT_ID);
	}

	@Test
	void activeLicenseGrantsOnlyFeaturesInItsOffer() {
		PlatformLicense active = license(5L, NOW.minusSeconds(60), null, LicenseState.ACTIVE);
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of(active));
		when(licenseRepository.findEnabledOffer(OFFER_ID)).thenReturn(Optional.of(offer));

		assertTrue(service.hasFeature(ESTABLISHMENT_ID, LicenseFeature.LEARNING_CONTENT, NOW));
		assertFalse(service.hasFeature(ESTABLISHMENT_ID, LicenseFeature.ATTENDANCE, NOW));
	}

	private PlatformLicense license(Long id, Instant from, Instant until, LicenseState ignoredState) {
		return new PlatformLicense(id, ESTABLISHMENT_ID, OFFER_ID, offer.code(), offer.name(), from,
			until, null, "admin", NOW);
	}
}
