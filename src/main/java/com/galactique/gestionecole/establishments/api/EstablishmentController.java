package com.galactique.gestionecole.establishments.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.establishments.application.EstablishmentApplicationService;
import com.galactique.gestionecole.establishments.application.EstablishmentOnboardingApplicationService;
import com.galactique.gestionecole.establishments.api.dto.EstablishmentOnboardingResponse;
import com.galactique.gestionecole.establishments.api.dto.OnboardEstablishmentRequest;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments")
public class EstablishmentController {
	private final EstablishmentApplicationService establishmentApplicationService;
	private final EstablishmentOnboardingApplicationService establishmentOnboardingApplicationService;
	private final EmployeeApplicationService employeeApplicationService;

	public EstablishmentController(EstablishmentApplicationService establishmentApplicationService,
		EstablishmentOnboardingApplicationService establishmentOnboardingApplicationService,
		EmployeeApplicationService employeeApplicationService) {
		this.establishmentApplicationService = establishmentApplicationService;
		this.establishmentOnboardingApplicationService = establishmentOnboardingApplicationService;
		this.employeeApplicationService = employeeApplicationService;
	}

	/** Retourne les établissements accessibles à l’appelant. */
	@GetMapping
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('can_establishment_read') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	public List<EstablishmentResponse> getEstablishments(Authentication authentication) {
		return employeeApplicationService.findEstablishments(toActor(authentication)).stream()
			.map(EstablishmentResponse::from)
			.toList();
	}

	/** Crée un établissement et invite son premier administrateur. */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN') and hasAuthority('can_establishment_onboard')")
	@ResponseStatus(HttpStatus.CREATED)
	public EstablishmentOnboardingResponse onboardEstablishment(@Valid @RequestBody OnboardEstablishmentRequest request,
		Authentication authentication) {
		return EstablishmentOnboardingResponse.from(
			establishmentOnboardingApplicationService.onboard(request, authentication.getName()));
	}

	/** Désactive un établissement ainsi que les accès de ses membres. */
	@DeleteMapping("/{establishmentId}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateEstablishment(@PathVariable UUID establishmentId) {
		establishmentApplicationService.deactivate(establishmentId);
	}

	private EmployeeActor toActor(Authentication authentication) {
		boolean globalAdministrator = hasAuthority(authentication, "ADMIN_ACCESS")
			|| hasAuthority(authentication, "can_establishment_read");
		boolean establishmentAdministrator = hasAuthority(authentication, "ESTABLISHMENT_STAFF_MANAGEMENT");
		return new EmployeeActor(authentication.getName(), globalAdministrator, establishmentAdministrator);
	}

	private boolean hasAuthority(Authentication authentication, String expectedAuthority) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority != null && expectedAuthority.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}