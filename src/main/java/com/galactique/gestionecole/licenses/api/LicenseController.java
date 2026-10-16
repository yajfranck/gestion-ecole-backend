package com.galactique.gestionecole.licenses.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.licenses.api.dto.AssignLicenseRequest;
import com.galactique.gestionecole.licenses.api.dto.CreateLicenseOfferRequest;
import com.galactique.gestionecole.licenses.api.dto.LicenseEventResponse;
import com.galactique.gestionecole.licenses.api.dto.LicenseOfferResponse;
import com.galactique.gestionecole.licenses.api.dto.PlatformLicenseResponse;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;

import jakarta.validation.Valid;

@RestController
public class LicenseController {
	private final LicenseApplicationService licenseApplicationService;

	public LicenseController(LicenseApplicationService licenseApplicationService) {
		this.licenseApplicationService = licenseApplicationService;
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/license")
	@PreAuthorize("hasAuthority('can_license_read') or hasRole('ADMIN')")
	public List<PlatformLicenseResponse> getEstablishmentLicenses(@PathVariable UUID establishmentId,
		Authentication authentication) {
		return licenseApplicationService.findLicenses(establishmentId, toActor(authentication)).stream()
			.map(PlatformLicenseResponse::from).toList();
	}

	@GetMapping("/api/v1/admin/license-offers")
	@PreAuthorize("hasRole('ADMIN')")
	public List<LicenseOfferResponse> getOffers() {
		return licenseApplicationService.findOffers().stream().map(LicenseOfferResponse::from).toList();
	}

	@PostMapping("/api/v1/admin/license-offers")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public LicenseOfferResponse createOffer(@Valid @RequestBody CreateLicenseOfferRequest request) {
		return LicenseOfferResponse.from(licenseApplicationService.createOffer(
			request.code(), request.name(), request.features()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses")
	@PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.CREATED)
	public PlatformLicenseResponse assign(@PathVariable UUID establishmentId,
		@Valid @RequestBody AssignLicenseRequest request, Authentication authentication) {
		return PlatformLicenseResponse.from(licenseApplicationService.assign(establishmentId,
			request.offerId(), request.validFrom(), request.validUntil(), authentication.getName()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/suspend")
	@PreAuthorize("hasRole('ADMIN')")
	public PlatformLicenseResponse suspend(@PathVariable UUID establishmentId, @PathVariable Long licenseId,
		Authentication authentication) {
		return PlatformLicenseResponse.from(licenseApplicationService.suspend(
			establishmentId, licenseId, authentication.getName()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/reactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public PlatformLicenseResponse reactivate(@PathVariable UUID establishmentId, @PathVariable Long licenseId,
		Authentication authentication) {
		return PlatformLicenseResponse.from(licenseApplicationService.reactivate(
			establishmentId, licenseId, authentication.getName()));
	}

	@PostMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/end")
	@PreAuthorize("hasRole('ADMIN')")
	public PlatformLicenseResponse end(@PathVariable UUID establishmentId, @PathVariable Long licenseId,
		Authentication authentication) {
		return PlatformLicenseResponse.from(licenseApplicationService.end(
			establishmentId, licenseId, authentication.getName()));
	}

	@GetMapping("/api/v1/admin/establishments/{establishmentId}/licenses/{licenseId}/events")
	@PreAuthorize("hasRole('ADMIN')")
	public List<LicenseEventResponse> getEvents(@PathVariable UUID establishmentId,
		@PathVariable Long licenseId) {
		return licenseApplicationService.findLicenses(establishmentId,
			new EmployeeActor("", true, false)).stream()
			.filter(license -> license.id().equals(licenseId))
			.findFirst()
			.map(license -> licenseApplicationService.findEvents(license.id()))
			.orElseThrow(com.galactique.gestionecole.licenses.application.LicenseNotFoundException::new)
			.stream().map(LicenseEventResponse::from).toList();
	}

	private EmployeeActor toActor(Authentication authentication) {
		boolean globalAdministrator = hasAuthority(authentication, "ADMIN_ACCESS")
			|| hasAuthority(authentication, "ROLE_ADMIN");
		return new EmployeeActor(authentication.getName(), globalAdministrator,
			hasAuthority(authentication, "ESTABLISHMENT_STAFF_MANAGEMENT"));
	}

	private boolean hasAuthority(Authentication authentication, String expectedAuthority) {
		return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
			.anyMatch(expectedAuthority::equals);
	}
}
