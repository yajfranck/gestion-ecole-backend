package com.galactique.gestionecole.schoolclasses.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.schoolclasses.application.CreateGradeLevelCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.UpdateGradeLevelCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/grade-levels")
public class GradeLevelController {
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;

	public GradeLevelController(SchoolCatalogApplicationService schoolCatalogApplicationService) {
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
	}

	/** Retourne les niveaux scolaires de l’établissement. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_grade_level_read')")
	public List<GradeLevelResponse> getGradeLevels(@PathVariable UUID establishmentId, Authentication authentication) {
		return schoolCatalogApplicationService.findGradeLevels(establishmentId, toActor(authentication)).stream()
			.map(GradeLevelResponse::from).toList();
	}

	/** Retourne un niveau scolaire identifié. */
	@GetMapping("/{gradeLevelId}")
	@PreAuthorize("hasAuthority('can_grade_level_read')")
	public GradeLevelResponse getGradeLevel(@PathVariable UUID establishmentId, @PathVariable UUID gradeLevelId,
		Authentication authentication) {
		return GradeLevelResponse.from(
			schoolCatalogApplicationService.findGradeLevel(establishmentId, gradeLevelId, toActor(authentication)));
	}

	/** Crée un niveau scolaire dans l’établissement. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_grade_level_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public GradeLevelResponse createGradeLevel(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreateGradeLevelRequest request, Authentication authentication) {
		return GradeLevelResponse.from(schoolCatalogApplicationService.createGradeLevel(establishmentId,
			new CreateGradeLevelCommand(request.code(), request.name(), request.displayOrder()), toActor(authentication)));
	}

	/** Modifie le code, le nom ou l’ordre d’affichage d’un niveau. */
	@PatchMapping("/{gradeLevelId}")
	@PreAuthorize("hasAuthority('can_grade_level_update')")
	public GradeLevelResponse updateGradeLevel(@PathVariable UUID establishmentId, @PathVariable UUID gradeLevelId,
		@Valid @RequestBody UpdateGradeLevelRequest request, Authentication authentication) {
		return GradeLevelResponse.from(schoolCatalogApplicationService.updateGradeLevel(establishmentId, gradeLevelId,
			new UpdateGradeLevelCommand(request.code(), request.name(), request.displayOrder()), toActor(authentication)));
	}

	/** Archive un niveau qui n’est plus référencé par une classe active. */
	@DeleteMapping("/{gradeLevelId}")
	@PreAuthorize("hasAuthority('can_grade_level_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveGradeLevel(@PathVariable UUID establishmentId, @PathVariable UUID gradeLevelId,
		Authentication authentication) {
		schoolCatalogApplicationService.archiveGradeLevel(establishmentId, gradeLevelId, toActor(authentication));
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