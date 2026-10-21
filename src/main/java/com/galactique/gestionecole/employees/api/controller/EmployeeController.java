package com.galactique.gestionecole.employees.api.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.api.dto.AcceptEmployeeInvitationRequest;
import com.galactique.gestionecole.employees.api.dto.CreateEmployeeRequest;
import com.galactique.gestionecole.employees.api.dto.EmployeeInvitationResponse;
import com.galactique.gestionecole.employees.api.dto.EmployeeResponse;
import com.galactique.gestionecole.employees.api.dto.UpdateEmployeeRequest;
import com.galactique.gestionecole.employees.api.dto.UpdateEmployeeRolesRequest;
import com.galactique.gestionecole.employees.application.CreateEmployeeCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.application.UpdateEmployeeCommand;
import com.galactique.gestionecole.employees.domain.Employee;

import jakarta.validation.Valid;

@RestController
public class EmployeeController {
	private final EmployeeApplicationService employeeApplicationService;

	public EmployeeController(EmployeeApplicationService employeeApplicationService) {
		this.employeeApplicationService = employeeApplicationService;
	}

	/** Invite un employé et déclenche l’envoi de son invitation. */
	@PostMapping("/api/v1/establishments/{establishmentId}/employees")
	@PreAuthorize("hasAuthority('can_employee_create') and (#request.directRoleCodes() == null or !#request.directRoleCodes().contains('ESTABLISHMENT_ADMIN') or hasAuthority('can_employee_assign_administrator'))")
	@ResponseStatus(HttpStatus.CREATED)
	public EmployeeInvitationResponse inviteEmployee(@PathVariable UUID establishmentId, @Valid @RequestBody CreateEmployeeRequest request,
		Authentication authentication) {
		Employee employee = employeeApplicationService.invite(
			new CreateEmployeeCommand(request.employeeNumber(), request.firstName(), request.lastName(), request.email(),
				request.positionId(), request.directRoleCodes() == null ? Set.of() : Set.copyOf(request.directRoleCodes())),
			toActor(authentication, establishmentId));
		return new EmployeeInvitationResponse(employee.id(), "INVITATION_SENT");
	}

	/** Modifie les informations et le poste d’un employé. */
	@PatchMapping("/api/v1/establishments/{establishmentId}/employees/{employeeId}")
	@PreAuthorize("hasAuthority('can_employee_update')")
	public EmployeeResponse updateEmployee(@PathVariable UUID establishmentId, @PathVariable UUID employeeId,
		@Valid @RequestBody UpdateEmployeeRequest request, Authentication authentication) {
		if (!request.hasChanges()) {
			throw new IllegalArgumentException("At least one employee field must be provided");
		}
		var command = new UpdateEmployeeCommand(
			request.employeeNumber(), request.firstName(), request.lastName(), request.email(), request.positionId());
		return EmployeeResponse.from(employeeApplicationService.updateProfile(employeeId, command, toActor(authentication, establishmentId)));
	}

	/** Remplace les rôles directs attribués à un employé. */
	@PutMapping("/api/v1/establishments/{establishmentId}/employees/{employeeId}/roles")
	@PreAuthorize("hasAuthority('can_employee_assign_roles') and (!#request.roleCodes().contains('ESTABLISHMENT_ADMIN') or hasAuthority('can_employee_assign_administrator'))")
	public EmployeeResponse updateEmployeeRoles(@PathVariable UUID establishmentId, @PathVariable UUID employeeId,
		@Valid @RequestBody UpdateEmployeeRolesRequest request, Authentication authentication) {
		return EmployeeResponse.from(
			employeeApplicationService.updateRoles(employeeId, request.roleCodes(), toActor(authentication, establishmentId)));
	}

	/** Désactive un employé et son compte sans supprimer son historique. */
	@DeleteMapping("/api/v1/establishments/{establishmentId}/employees/{employeeId}")
	@PreAuthorize("hasAuthority('can_employee_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateEmployee(@PathVariable UUID establishmentId, @PathVariable UUID employeeId, Authentication authentication) {
		employeeApplicationService.deactivateEmployee(employeeId, toActor(authentication, establishmentId));
	}

	/** Accepte une invitation et crée le compte de l’employé. */
	@PostMapping("/api/v1/employees/invitations/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acceptInvitation(@Valid @RequestBody AcceptEmployeeInvitationRequest request) {
		employeeApplicationService.acceptInvitation(request.token(), request.password());
	}

	/** Retourne les employés visibles dans le périmètre de l’appelant. */
	@GetMapping("/api/v1/establishments/{establishmentId}/employees")
	@PreAuthorize("hasAuthority('can_employee_read')")
	public List<EmployeeResponse> getEmployee(@PathVariable UUID establishmentId, Authentication authentication) {
		return employeeApplicationService.findAll(toActor(authentication, establishmentId)).stream().map(EmployeeResponse::from).toList();
	}

	/** Retourne la fiche de l’employé actuellement authentifié. */
	@GetMapping("/api/v1/establishments/{establishmentId}/employees/me")
	@PreAuthorize("hasAuthority('can_employee_read_self')")
	public EmployeeResponse getCurrentEmployee(@PathVariable UUID establishmentId, Authentication authentication) {
		return EmployeeResponse.from(employeeApplicationService.findCurrent(authentication.getName(), establishmentId));
	}

	/** Retourne la fiche d’un employé identifié. */
	@GetMapping("/api/v1/establishments/{establishmentId}/employees/{employeeId}")
	@PreAuthorize("hasAuthority('can_employee_read')")
	public EmployeeResponse getEmployee(@PathVariable UUID establishmentId, @PathVariable UUID employeeId, Authentication authentication) {
		return EmployeeResponse.from(employeeApplicationService.findById(employeeId, toActor(authentication, establishmentId)));
	}

	private EmployeeActor toActor(Authentication authentication, UUID establishmentId) {
		return new EmployeeActor(authentication.getName(), establishmentId);
	}
}
