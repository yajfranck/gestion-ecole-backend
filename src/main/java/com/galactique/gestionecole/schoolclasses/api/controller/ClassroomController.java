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
import com.galactique.gestionecole.schoolclasses.api.dto.ClassroomResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.CreateClassroomRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.UpdateClassroomRequest;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/classrooms")
public class ClassroomController {
	private final SchoolCatalogApplicationService catalog;
	public ClassroomController(SchoolCatalogApplicationService catalog) { this.catalog = catalog; }

	@GetMapping
	@PreAuthorize("hasAuthority('can_school_class_read')")
	public List<ClassroomResponse> list(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, Authentication authentication) {
		return catalog.findClassrooms(establishmentId, academicYearId, schoolClassId, actor(authentication))
			.stream().map(ClassroomResponse::from).toList();
	}

	@GetMapping("/{classroomId}")
	@PreAuthorize("hasAuthority('can_school_class_read')")
	public ClassroomResponse get(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, Authentication authentication) {
		return ClassroomResponse.from(catalog.findClassroom(establishmentId, academicYearId, schoolClassId,
			classroomId, actor(authentication)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('can_school_class_create')")
	public ClassroomResponse create(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @Valid @RequestBody CreateClassroomRequest request, Authentication authentication) {
		return ClassroomResponse.from(catalog.createClassroom(establishmentId, academicYearId, schoolClassId,
			request.code(), request.name(), request.maxStudents(), actor(authentication)));
	}

	@PatchMapping("/{classroomId}")
	@PreAuthorize("hasAuthority('can_school_class_update')")
	public ClassroomResponse update(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId,
		@Valid @RequestBody UpdateClassroomRequest request, Authentication authentication) {
		return ClassroomResponse.from(catalog.updateClassroom(establishmentId, academicYearId, schoolClassId,
			classroomId, request.code(), request.name(), request.maxStudents(), actor(authentication)));
	}

	@DeleteMapping("/{classroomId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('can_school_class_delete')")
	public void archive(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, Authentication authentication) {
		catalog.archiveClassroom(establishmentId, academicYearId, schoolClassId, classroomId, actor(authentication));
	}

	private EmployeeActor actor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
