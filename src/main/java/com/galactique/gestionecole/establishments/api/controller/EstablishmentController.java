package com.galactique.gestionecole.establishments.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.establishments.api.dto.EstablishmentOnboardingResponse;
import com.galactique.gestionecole.establishments.api.dto.EstablishmentResponse;
import com.galactique.gestionecole.establishments.api.dto.OnboardEstablishmentRequest;
import com.galactique.gestionecole.establishments.api.dto.UpdateEstablishmentRequest;
import com.galactique.gestionecole.establishments.application.EstablishmentApplicationService;
import com.galactique.gestionecole.establishments.application.EstablishmentOnboardingApplicationService;
import com.galactique.gestionecole.establishments.application.EstablishmentVisibilityApplicationService;
import com.galactique.gestionecole.security.application.CallerScopeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments")
public class EstablishmentController {
	private final CallerScopeService callerScope;
	private final EstablishmentApplicationService establishmentApplicationService;
	private final EstablishmentOnboardingApplicationService establishmentOnboardingApplicationService;
	private final EstablishmentVisibilityApplicationService establishmentVisibilityApplicationService;

	public EstablishmentController(EstablishmentApplicationService establishmentApplicationService,
		EstablishmentOnboardingApplicationService establishmentOnboardingApplicationService,
		EstablishmentVisibilityApplicationService establishmentVisibilityApplicationService, CallerScopeService callerScope) {
		this.callerScope = callerScope;
		this.establishmentApplicationService = establishmentApplicationService;
		this.establishmentOnboardingApplicationService = establishmentOnboardingApplicationService;
		this.establishmentVisibilityApplicationService = establishmentVisibilityApplicationService;
	}

	/** Retourne les établissements accessibles à l’appelant. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_establishment_read')")
	public List<EstablishmentResponse> getEstablishments(Authentication authentication) {
		return establishmentVisibilityApplicationService.findAccessible(authentication.getName()).stream()
			.map(EstablishmentResponse::from)
			.toList();
	}

	/** Crée un établissement et invite son premier administrateur. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_establishment_onboard')")
	@ResponseStatus(HttpStatus.CREATED)
	public EstablishmentOnboardingResponse onboardEstablishment(@Valid @RequestBody OnboardEstablishmentRequest request,
		Authentication authentication) {
		callerScope.requirePlatformScope(authentication.getName());
		return EstablishmentOnboardingResponse.from(
			establishmentOnboardingApplicationService.onboard(request, authentication.getName()));
	}

	/** Modifie le code et/ou le nom d’un établissement actif. */
	@PatchMapping("/{establishmentId}")
	@PreAuthorize("hasAuthority('can_establishment_update')")
	public EstablishmentResponse updateEstablishment(@PathVariable UUID establishmentId,
		@RequestBody UpdateEstablishmentRequest request, Authentication authentication) {
		return EstablishmentResponse.from(establishmentApplicationService.update(establishmentId,
			request.toCommand(), authentication.getName()));
	}

	/** Désactive un établissement ainsi que les accès de ses membres. */
	@DeleteMapping("/{establishmentId}")
	@PreAuthorize("hasAuthority('can_establishment_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateEstablishment(@PathVariable UUID establishmentId, Authentication authentication) {
		callerScope.requireEstablishment(authentication.getName(), establishmentId);
		establishmentApplicationService.deactivate(establishmentId);
	}

}
