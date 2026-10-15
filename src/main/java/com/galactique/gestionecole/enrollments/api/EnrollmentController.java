package com.galactique.gestionecole.enrollments.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.enrollments.application.CreateEnrollmentCommand;
import com.galactique.gestionecole.enrollments.application.EnrollmentApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/enrollments")
public class EnrollmentController {
	private final EnrollmentApplicationService enrollmentApplicationService;

	public EnrollmentController(EnrollmentApplicationService enrollmentApplicationService) {
		this.enrollmentApplicationService = enrollmentApplicationService;
	}

	/** Retourne les inscriptions conservées pour une année scolaire. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_enrollment_read')")
	public List<EnrollmentResponse> getEnrollments(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return enrollmentApplicationService.findAll(establishmentId, academicYearId, toActor(authentication)).stream()
			.map(EnrollmentResponse::from).toList();
	}

	/** Inscrit un élève dans une classe pour l’année scolaire indiquée. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_enrollment_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentResponse createEnrollment(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @Valid @RequestBody CreateEnrollmentRequest request,
		Authentication authentication) {
		return EnrollmentResponse.from(enrollmentApplicationService.create(establishmentId, academicYearId,
			new CreateEnrollmentCommand(request.studentId(), request.schoolClassId()), toActor(authentication)));
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