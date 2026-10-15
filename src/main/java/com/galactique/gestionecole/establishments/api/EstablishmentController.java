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
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments")
public class EstablishmentController {
	private final EstablishmentApplicationService establishmentApplicationService;
	private final EmployeeApplicationService employeeApplicationService;

	public EstablishmentController(EstablishmentApplicationService establishmentApplicationService,
		EmployeeApplicationService employeeApplicationService) {
		this.establishmentApplicationService = establishmentApplicationService;
		this.employeeApplicationService = employeeApplicationService;
	}

	/** Retourne les établissements accessibles à l’appelant. */
	@GetMapping
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	public List<EstablishmentResponse> getEstablishments(Authentication authentication) {
		return employeeApplicationService.findEstablishments(toActor(authentication)).stream()
			.map(EstablishmentResponse::from)
			.toList();
	}

	/** Crée un établissement à partir de son code et de son nom. */
	@PostMapping
	@PreAuthorize("hasAuthority('ADMIN_ACCESS')")
	@ResponseStatus(HttpStatus.CREATED)
	public EstablishmentResponse createEstablishment(@Valid @RequestBody CreateEstablishmentRequest request) {
		Establishment establishment = establishmentApplicationService.create(request.code(), request.name());
		return EstablishmentResponse.from(establishment);
	}

	/** Désactive un établissement ainsi que les accès de ses membres. */
	@DeleteMapping("/{establishmentId}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateEstablishment(@PathVariable UUID establishmentId) {
		establishmentApplicationService.deactivate(establishmentId);
	}

	private EmployeeActor toActor(Authentication authentication) {
		boolean globalAdministrator = hasAuthority(authentication, "ADMIN_ACCESS");
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