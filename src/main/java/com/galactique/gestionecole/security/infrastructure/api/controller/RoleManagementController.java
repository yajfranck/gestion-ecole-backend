package com.galactique.gestionecole.security.infrastructure.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.security.application.CreateRoleCommand;
import com.galactique.gestionecole.security.application.RoleManagementApplicationService;
import com.galactique.gestionecole.security.application.UpdateRoleCommand;
import com.galactique.gestionecole.security.infrastructure.api.dto.CreateRoleRequest;
import com.galactique.gestionecole.security.infrastructure.api.dto.ManagedRoleResponse;
import com.galactique.gestionecole.security.infrastructure.api.dto.UpdateRoleRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/role")
public class RoleManagementController {
	private final RoleManagementApplicationService roleManagementApplicationService;

	public RoleManagementController(RoleManagementApplicationService roleManagementApplicationService) {
		this.roleManagementApplicationService = roleManagementApplicationService;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ADMIN_ACCESS')")
	@ResponseStatus(HttpStatus.CREATED)
	public ManagedRoleResponse createRole(@Valid @RequestBody CreateRoleRequest request) {
		return ManagedRoleResponse.from(roleManagementApplicationService.create(new CreateRoleCommand(
			request.code(), request.name(), request.description(), request.permissionCodes())));
	}

	@PutMapping("/{code}")
	@PreAuthorize("hasAuthority('ADMIN_ACCESS')")
	public ManagedRoleResponse updateRole(@PathVariable String code, @Valid @RequestBody UpdateRoleRequest request) {
		return ManagedRoleResponse.from(roleManagementApplicationService.update(code, new UpdateRoleCommand(
			code, request.name(), request.description(), request.permissionCodes())));
	}
}