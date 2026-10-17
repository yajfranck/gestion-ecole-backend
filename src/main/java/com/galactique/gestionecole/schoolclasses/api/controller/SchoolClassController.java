package com.galactique.gestionecole.schoolclasses.api.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.schoolclasses.api.dto.CreateSchoolClassRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.UpdateSchoolClassRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.SchoolClassResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.SchoolClassSubjectResponse;
import com.galactique.gestionecole.schoolclasses.application.CreateSchoolClassCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.UpdateSchoolClassCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes")
public class SchoolClassController {
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;

	public SchoolClassController(SchoolCatalogApplicationService schoolCatalogApplicationService) {
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
	}

	/** Retourne les classes actives d’une année scolaire. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_school_class_read')")
	public List<SchoolClassResponse> getClasses(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return schoolCatalogApplicationService.findClasses(establishmentId, academicYearId, toActor(authentication))
			.stream().map(SchoolClassResponse::from).toList();
	}

	/** Retourne une classe identifiée dans une année scolaire. */
	@GetMapping("/{schoolClassId}")
	@PreAuthorize("hasAuthority('can_school_class_read')")
	public SchoolClassResponse getClass(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, Authentication authentication) {
		return SchoolClassResponse.from(schoolCatalogApplicationService.findClass(
			establishmentId, academicYearId, schoolClassId, toActor(authentication)));
	}

	/** Crée une classe dans l’année scolaire. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_school_class_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public SchoolClassResponse createClass(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@Valid @RequestBody CreateSchoolClassRequest request, Authentication authentication) {
		return SchoolClassResponse.from(schoolCatalogApplicationService.createClass(establishmentId, academicYearId,
			new CreateSchoolClassCommand(request.code(), request.name(), request.series()), toActor(authentication)));
	}

	/** Modifie le code, le nom ou la série facultative d’une classe. */
	@PatchMapping("/{schoolClassId}")
	@PreAuthorize("hasAuthority('can_school_class_update')")
	public SchoolClassResponse updateClass(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @Valid @RequestBody UpdateSchoolClassRequest request,
		Authentication authentication) {
		return SchoolClassResponse.from(schoolCatalogApplicationService.updateClass(establishmentId, academicYearId,
			schoolClassId, new UpdateSchoolClassCommand(request.code(), request.name(), request.series()),
			toActor(authentication)));
	}

	/** Archive une classe sans supprimer ses données historiques. */
	@DeleteMapping("/{schoolClassId}")
	@PreAuthorize("hasAuthority('can_school_class_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveClass(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, Authentication authentication) {
		schoolCatalogApplicationService.archiveClass(establishmentId, academicYearId, schoolClassId,
			toActor(authentication));
	}

	/** Retourne les matières affectées à une classe. */
	@GetMapping("/{schoolClassId}/subjects")
	@PreAuthorize("hasAuthority('can_class_subject_read')")
	public List<SchoolClassSubjectResponse> getClassSubjects(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId, Authentication authentication) {
		return schoolCatalogApplicationService.findClassSubjects(establishmentId, academicYearId, schoolClassId,
			toActor(authentication)).stream().map(SchoolClassSubjectResponse::from).toList();
	}

	/** Affecte ou réactive une matière pour une classe. */
	@PutMapping("/{schoolClassId}/subjects/{subjectId}")
	@PreAuthorize("hasAuthority('can_class_subject_assign')")
	public SchoolClassSubjectResponse assignSubject(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId, @PathVariable UUID subjectId,
		Authentication authentication) {
		return SchoolClassSubjectResponse.from(schoolCatalogApplicationService.assignSubject(establishmentId,
			academicYearId, schoolClassId, subjectId, toActor(authentication)));
	}

	/** Archive l’affectation d’une matière à une classe. */
	@DeleteMapping("/{schoolClassId}/subjects/{subjectId}")
	@PreAuthorize("hasAuthority('can_class_subject_unassign')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unassignSubject(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID subjectId, Authentication authentication) {
		schoolCatalogApplicationService.unassignSubject(establishmentId, academicYearId, schoolClassId,
			subjectId, toActor(authentication));
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
