package com.galactique.gestionecole.schoolclasses.api.controller;

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

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.schoolclasses.api.dto.CreateSubjectsRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.SubjectResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.UpdateSubjectRequest;
import com.galactique.gestionecole.schoolclasses.application.CreateSubjectCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.UpdateSubjectCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/subjects")
public class SubjectController {
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;

	public SubjectController(SchoolCatalogApplicationService schoolCatalogApplicationService) {
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
	}

	/** Retourne les matières de l’établissement. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_subject_read')")
	public List<SubjectResponse> getSubjects(@PathVariable UUID establishmentId, Authentication authentication) {
		return schoolCatalogApplicationService.findSubjects(establishmentId, toActor(authentication)).stream()
			.map(SubjectResponse::from).toList();
	}

	/** Retourne une matière identifiée. */
	@GetMapping("/{subjectId}")
	@PreAuthorize("hasAuthority('can_subject_read')")
	public SubjectResponse getSubject(@PathVariable UUID establishmentId, @PathVariable UUID subjectId,
		Authentication authentication) {
		return SubjectResponse.from(schoolCatalogApplicationService.findSubject(
			establishmentId, subjectId, toActor(authentication)));
	}

	/** Crée plusieurs matières dans une seule transaction. */
	@PostMapping({"", "/batch"})
	@PreAuthorize("hasAuthority('can_subject_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public List<SubjectResponse> createSubjects(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreateSubjectsRequest request, Authentication authentication) {
		return schoolCatalogApplicationService.createSubjects(establishmentId,
			request.subjects().stream().map(subject -> new CreateSubjectCommand(subject.code(), subject.name())).toList(),
			toActor(authentication)).stream().map(SubjectResponse::from).toList();
	}

	/** Modifie le nom d’une matière. */
	@PatchMapping("/{subjectId}")
	@PreAuthorize("hasAuthority('can_subject_update')")
	public SubjectResponse updateSubject(@PathVariable UUID establishmentId, @PathVariable UUID subjectId,
		@Valid @RequestBody UpdateSubjectRequest request, Authentication authentication) {
		return SubjectResponse.from(schoolCatalogApplicationService.updateSubject(establishmentId, subjectId,
			new UpdateSubjectCommand(request.code(), request.name()), toActor(authentication)));
	}

	/** Archive une matière qui n’est plus utilisée par une classe active. */
	@DeleteMapping("/{subjectId}")
	@PreAuthorize("hasAuthority('can_subject_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveSubject(@PathVariable UUID establishmentId, @PathVariable UUID subjectId,
		Authentication authentication) {
		schoolCatalogApplicationService.archiveSubject(establishmentId, subjectId, toActor(authentication));
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
