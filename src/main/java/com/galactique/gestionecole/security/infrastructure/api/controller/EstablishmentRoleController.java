package com.galactique.gestionecole.security.infrastructure.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.security.application.CreateRoleCommand;
import com.galactique.gestionecole.security.application.EstablishmentRoleApplicationService;
import com.galactique.gestionecole.security.application.UpdateRoleCommand;
import com.galactique.gestionecole.security.infrastructure.api.PermissionResponse;
import com.galactique.gestionecole.security.infrastructure.api.dto.CreateRoleRequest;
import com.galactique.gestionecole.security.infrastructure.api.dto.EstablishmentRoleResponse;
import com.galactique.gestionecole.security.infrastructure.api.dto.UpdateRoleRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}")
public class EstablishmentRoleController {
	private final EstablishmentRoleApplicationService establishmentRoleApplicationService;

	public EstablishmentRoleController(EstablishmentRoleApplicationService establishmentRoleApplicationService) {
		this.establishmentRoleApplicationService = establishmentRoleApplicationService;
	}

	@GetMapping("/roles")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('can_role_read')")
	public List<EstablishmentRoleResponse> getRoles(@PathVariable UUID establishmentId, Authentication authentication) {
		return establishmentRoleApplicationService.findRoles(establishmentId, toActor(authentication)).stream()
			.map(EstablishmentRoleResponse::from).toList();
	}

	@PostMapping("/roles")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('can_role_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public EstablishmentRoleResponse createRole(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreateRoleRequest request, Authentication authentication) {
		return EstablishmentRoleResponse.from(establishmentRoleApplicationService.createRole(establishmentId,
			new CreateRoleCommand(request.code(), request.name(), request.description(), request.permissionCodes()),
			toActor(authentication)));
	}

	@PutMapping("/roles/{code}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('can_role_update')")
	public EstablishmentRoleResponse updateRole(@PathVariable UUID establishmentId, @PathVariable String code,
		@Valid @RequestBody UpdateRoleRequest request, Authentication authentication) {
		return EstablishmentRoleResponse.from(establishmentRoleApplicationService.updateRole(establishmentId, code,
			new UpdateRoleCommand(code, request.name(), request.description(), request.permissionCodes()),
			toActor(authentication)));
	}

	@GetMapping("/permissions")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS') or hasAuthority('can_permission_read')")
	public List<PermissionResponse> getPermissions(@PathVariable UUID establishmentId, Authentication authentication) {
		return establishmentRoleApplicationService.findPermissions(establishmentId, toActor(authentication)).stream()
			.map(permission -> new PermissionResponse(permission.code(), permission.description())).toList();
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName(), hasAuthority(authentication, "ADMIN_ACCESS"),
			hasAuthority(authentication, "ESTABLISHMENT_STAFF_MANAGEMENT"));
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