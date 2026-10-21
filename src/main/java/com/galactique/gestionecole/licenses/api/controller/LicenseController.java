package com.galactique.gestionecole.licenses.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.licenses.api.dto.AssignLicenseRequest;
import com.galactique.gestionecole.licenses.api.dto.CreateLicenseOfferRequest;
import com.galactique.gestionecole.licenses.api.dto.LicenseEventResponse;
import com.galactique.gestionecole.licenses.api.dto.LicenseOfferResponse;
import com.galactique.gestionecole.licenses.api.dto.PlatformLicenseResponse;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.application.LicenseNotFoundException;
import com.galactique.gestionecole.security.application.CallerScopeService;

import jakarta.validation.Valid;

@RestController
public class LicenseController {
	private final CallerScopeService callerScope;
	private final LicenseApplicationService licenseApplicationService;

	public LicenseController(LicenseApplicationService licenseApplicationService, CallerScopeService callerScope) {
		this.callerScope = callerScope;
		this.licenseApplicationService = licenseApplicationService;
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/license")
	@PreAuthorize("hasAuthority('can_license_read')")
	public List<PlatformLicenseResponse> getEstablishmentLicenses(@PathVariable UUID establishmentId,
		Authentication authentication) {
		return licenseApplicationService.findLicenses(establishmentId, toActor(authentication)).stream()
			.map(PlatformLicenseResponse::from).toList();
	}

	@GetMapping("/api/v1/admin/license-offers")
	@PreAuthorize("hasAuthority('can_license_offer_read')")
	public List<LicenseOfferResponse> getOffers(Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return licenseApplicationService.findOffers().stream().map(LicenseOfferResponse::from).toList();
	}

	@PostMapping("/api/v1/admin/license-offers")
	@PreAuthorize("hasAuthority('can_license_offer_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public LicenseOfferResponse createOffer(@Valid @RequestBody CreateLicenseOfferRequest request, Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return LicenseOfferResponse.from(licenseApplicationService.createOffer(
			request.code(), request.name(), request.features()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses")
	@PreAuthorize("hasAuthority('can_license_assign')")
	@ResponseStatus(HttpStatus.CREATED)
	public PlatformLicenseResponse assign(@PathVariable UUID establishmentId,
		@Valid @RequestBody AssignLicenseRequest request, Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return PlatformLicenseResponse.from(licenseApplicationService.assign(establishmentId,
			request.offerId(), request.validFrom(), request.validUntil(), authentication.getName()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/suspend")
	@PreAuthorize("hasAuthority('can_license_suspend')")
	public PlatformLicenseResponse suspend(@PathVariable UUID establishmentId, @PathVariable Long licenseId,
		Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return PlatformLicenseResponse.from(licenseApplicationService.suspend(
			establishmentId, licenseId, authentication.getName()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/reactivate")
	@PreAuthorize("hasAuthority('can_license_reactivate')")
	public PlatformLicenseResponse reactivate(@PathVariable UUID establishmentId, @PathVariable Long licenseId,
		Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return PlatformLicenseResponse.from(licenseApplicationService.reactivate(
			establishmentId, licenseId, authentication.getName()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/end")
	@PreAuthorize("hasAuthority('can_license_end')")
	public PlatformLicenseResponse end(@PathVariable UUID establishmentId, @PathVariable Long licenseId,
		Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return PlatformLicenseResponse.from(licenseApplicationService.end(
			establishmentId, licenseId, authentication.getName()));
	}

	@GetMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/events")
	@PreAuthorize("hasAuthority('can_license_event_read')")
	public List<LicenseEventResponse> getEvents(@PathVariable UUID establishmentId,
		@PathVariable Long licenseId, Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return licenseApplicationService.findLicenses(establishmentId,
			toActor(authentication)).stream()
			.filter(license -> license.id().equals(licenseId))
			.findFirst()
			.map(license -> licenseApplicationService.findEvents(license.id()))
			.orElseThrow(LicenseNotFoundException::new)
			.stream().map(LicenseEventResponse::from).toList();
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
