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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.establishments.application.EstablishmentApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseOffer;
import com.galactique.gestionecole.licenses.domain.LicenseRepository;
import com.galactique.gestionecole.licenses.domain.LicenseState;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;
import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.UserAccount;
import com.galactique.gestionecole.security.domain.UserAccountRepository;

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

	@Mock
	private UserAccountRepository userAccountRepository;

	private LicenseApplicationService service;
	private LicenseOffer offer;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
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

	@AfterEach
	void clearAuthentication() { SecurityContextHolder.clearContext(); }

	@Test
	void authenticatedGlobalAdminStillRequiresAnActiveLicense() {
		authenticate("operator", "ROLE_ADMIN");
		for (LicenseFeature feature : LicenseFeature.values()) {
			assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, feature, NOW), feature.name());
		}
		verify(licenseRepository, org.mockito.Mockito.atLeastOnce()).findLicenses(ESTABLISHMENT_ID);
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of());
		assertFalse(service.hasFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
	}

	@Test
	void establishmentAdminAndLegacyAdminAccessAreStillSubjectToLicenseRestrictions() {
		authenticate("operator", "ADMIN_ACCESS");
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of());
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.LEARNING_CONTENT, NOW));
		PlatformLicense suspended = new PlatformLicense(8L, ESTABLISHMENT_ID, OFFER_ID, offer.code(), offer.name(),
			NOW.minusSeconds(60), null, NOW.minusSeconds(1), "admin", NOW);
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of(suspended));
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.LEARNING_CONTENT, NOW));
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of(license(9L, NOW.minusSeconds(60), NOW.minusSeconds(1), LicenseState.EXPIRED)));
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.LEARNING_CONTENT, NOW));
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of(license(10L, NOW.minusSeconds(60), null, LicenseState.ACTIVE)));
		when(licenseRepository.findEnabledOffer(OFFER_ID)).thenReturn(Optional.of(offer));
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
		assertTrue(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.LEARNING_CONTENT, NOW));
	}

	@Test
	void staleAdminClaimsDisabledAccountsAndScopedRolesDoNotGrantAnExemption() {
		authenticate("operator", "ROLE_ADMIN");
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of());
		for (UserAccount account : List.of(account("STAFF", true, true, false, null),
			account("ADMIN", false, true, false, null), account("ADMIN", true, false, false, null),
			account("ADMIN", true, true, true, null), account("ADMIN", true, true, false, ESTABLISHMENT_ID))) {
			assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
		}
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
	}

	@Test
	void anonymousAndUnauthenticatedContextsDoNotGrantAnExemption() {
		when(licenseRepository.findLicenses(ESTABLISHMENT_ID)).thenReturn(List.of());
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("operator", "ignored"));
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "operator", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		assertFalse(service.canAccessFeature(ESTABLISHMENT_ID, LicenseFeature.TUITION, NOW));
		verify(userAccountRepository, never()).findByUsername(any());
	}

	private void authenticate(String username, String authority) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, "ignored",
			List.of(new SimpleGrantedAuthority(authority))));
	}
	private UserAccount account(String roleCode, boolean roleEnabled, boolean enabled, boolean locked, UUID scope) {
		var role = new Role(roleCode, "Operator role", null, roleEnabled, Set.of(new Permission("can_user_read", "Read users")), scope);
		return new UserAccount(UUID.randomUUID(), "operator", "hash", enabled, locked, NOW, NOW, null, Set.of(role));
	}

	private PlatformLicense license(Long id, Instant from, Instant until, LicenseState ignoredState) {
		return new PlatformLicense(id, ESTABLISHMENT_ID, OFFER_ID, offer.code(), offer.name(), from,
			until, null, "admin", NOW);
	}
}
