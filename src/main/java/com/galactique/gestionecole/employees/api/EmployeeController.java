package com.galactique.gestionecole.employees.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.CreateEmployeeCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.domain.Employee;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
	private final EmployeeApplicationService employeeApplicationService;

	public EmployeeController(EmployeeApplicationService employeeApplicationService) {
		this.employeeApplicationService = employeeApplicationService;
	}

	/** Invite un employé et déclenche l’envoi de son invitation. */
	@PostMapping
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	@ResponseStatus(HttpStatus.CREATED)
	public EmployeeInvitationResponse inviteEmployee(@Valid @RequestBody CreateEmployeeRequest request,
		Authentication authentication) {
		Employee employee = employeeApplicationService.invite(
			new CreateEmployeeCommand(request.employeeNumber(), request.firstName(), request.lastName(), request.email(),
				request.positionId(), request.directRoleCodes() == null ? Set.of() : Set.copyOf(request.directRoleCodes())),
			toActor(authentication));
		return new EmployeeInvitationResponse(employee.id(), "INVITATION_SENT");
	}

	/** Modifie les informations et le poste d’un employé. */
	@PatchMapping("/{employeeId}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	public EmployeeResponse updateEmployee(@PathVariable UUID employeeId,
		@Valid @RequestBody UpdateEmployeeRequest request, Authentication authentication) {
		if (!request.hasChanges()) {
			throw new IllegalArgumentException("At least one employee field must be provided");
		}
		var command = new com.galactique.gestionecole.employees.application.UpdateEmployeeCommand(
			request.employeeNumber(), request.firstName(), request.lastName(), request.email(), request.positionId());
		return EmployeeResponse.from(employeeApplicationService.updateProfile(employeeId, command, toActor(authentication)));
	}

	/** Remplace les rôles directs attribués à un employé. */
	@PutMapping("/{employeeId}/roles")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	public EmployeeResponse updateEmployeeRoles(@PathVariable UUID employeeId,
		@Valid @RequestBody UpdateEmployeeRolesRequest request, Authentication authentication) {
		return EmployeeResponse.from(
			employeeApplicationService.updateRoles(employeeId, request.roleCodes(), toActor(authentication)));
	}

	/** Désactive un employé et son compte sans supprimer son historique. */
	@DeleteMapping("/{employeeId}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateEmployee(@PathVariable UUID employeeId, Authentication authentication) {
		employeeApplicationService.deactivateEmployee(employeeId, toActor(authentication));
	}

	/** Accepte une invitation et crée le compte de l’employé. */
	@PostMapping("/invitations/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acceptInvitation(@Valid @RequestBody AcceptEmployeeInvitationRequest request) {
		employeeApplicationService.acceptInvitation(request.token(), request.password());
	}

	/** Retourne les employés visibles dans le périmètre de l’appelant. */
	@GetMapping
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	public List<EmployeeResponse> getEmployee(Authentication authentication) {
		return employeeApplicationService.findAll(toActor(authentication)).stream().map(EmployeeResponse::from).toList();
	}

	/** Retourne la fiche de l’employé actuellement authentifié. */
	@GetMapping("/me")
	@PreAuthorize("hasAuthority('STAFF_ACCESS')")
	public EmployeeResponse getCurrentEmployee(Authentication authentication) {
		return EmployeeResponse.from(employeeApplicationService.findCurrent(authentication.getName()));
	}

	/** Retourne la fiche d’un employé identifié. */
	@GetMapping("/{employeeId}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('ESTABLISHMENT_STAFF_MANAGEMENT')")
	public EmployeeResponse getEmployee(@PathVariable UUID employeeId, Authentication authentication) {
		return EmployeeResponse.from(employeeApplicationService.findById(employeeId, toActor(authentication)));
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