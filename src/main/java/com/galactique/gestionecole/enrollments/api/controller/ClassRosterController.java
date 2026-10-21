package com.galactique.gestionecole.enrollments.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.enrollments.api.dto.ClassStudentResponse;
import com.galactique.gestionecole.enrollments.application.EnrollmentApplicationService;

@RestController
public class ClassRosterController {
	private final EnrollmentApplicationService enrollmentApplicationService;

	public ClassRosterController(EnrollmentApplicationService enrollmentApplicationService) {
		this.enrollmentApplicationService = enrollmentApplicationService;
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classes/{schoolClassId}/students")
	@PreAuthorize("hasAuthority('can_enrollment_read')")
	public List<ClassStudentResponse> getClassStudents(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId,
		@RequestParam(required = false) UUID classroomId, Authentication authentication) {
		return enrollmentApplicationService.findStudentsByClass(establishmentId, academicYearId,
			schoolClassId, classroomId, toActor(authentication)).stream().map(ClassStudentResponse::from).toList();
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
