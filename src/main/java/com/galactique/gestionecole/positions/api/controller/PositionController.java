package com.galactique.gestionecole.positions.api.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.positions.api.dto.CreatePositionRequest;
import com.galactique.gestionecole.positions.api.dto.PositionResponse;
import com.galactique.gestionecole.positions.api.dto.UpdatePositionRequest;
import com.galactique.gestionecole.positions.api.dto.UpdatePositionRolesRequest;
import com.galactique.gestionecole.positions.application.PositionApplicationService;
import com.galactique.gestionecole.positions.domain.Position;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/positions")
public class PositionController {
	private final PositionApplicationService positionApplicationService;

	public PositionController(PositionApplicationService positionApplicationService) {
		this.positionApplicationService = positionApplicationService;
	}

	/** Retourne les postes actifs d’un établissement. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_position_read')")
	public List<PositionResponse> getPositions(@PathVariable UUID establishmentId, Authentication authentication) {
		return positionApplicationService.findAll(establishmentId, toActor(authentication)).stream()
			.map(PositionResponse::from)
			.toList();
	}

	/** Crée un poste et lui attribue éventuellement des rôles. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_position_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public PositionResponse createPosition(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreatePositionRequest request, Authentication authentication) {
		Set<String> roleCodes = request.roleCodes() == null ? Set.of() : Set.copyOf(request.roleCodes());
		Position position = positionApplicationService.create(
			establishmentId, request.name(), roleCodes, toActor(authentication));
		return PositionResponse.from(position);
	}

	/** Renomme un poste existant. */
	@PatchMapping("/{positionId}")
	@PreAuthorize("hasAuthority('can_position_update')")
	public PositionResponse updatePosition(@PathVariable UUID establishmentId, @PathVariable UUID positionId,
		@Valid @RequestBody UpdatePositionRequest request, Authentication authentication) {
		return PositionResponse.from(positionApplicationService.updateName(
			establishmentId, positionId, request.name(), toActor(authentication)));
	}

	/** Remplace les rôles hérités par les titulaires du poste. */
	@PutMapping("/{positionId}/roles")
	@PreAuthorize("hasAuthority('can_position_assign_roles')")
	public PositionResponse updatePositionRoles(@PathVariable UUID establishmentId, @PathVariable UUID positionId,
		@Valid @RequestBody UpdatePositionRolesRequest request, Authentication authentication) {
		Set<String> roleCodes = request.roleCodes() == null ? Set.of() : Set.copyOf(request.roleCodes());
		return PositionResponse.from(positionApplicationService.updateRoles(
			establishmentId, positionId, roleCodes, toActor(authentication)));
	}

	/** Désactive un poste qui n’est plus rattaché à des employés. */
	@DeleteMapping("/{positionId}")
	@PreAuthorize("hasAuthority('can_position_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivatePosition(@PathVariable UUID establishmentId, @PathVariable UUID positionId,
		Authentication authentication) {
		positionApplicationService.disable(establishmentId, positionId, toActor(authentication));
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
