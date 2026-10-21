package com.galactique.gestionecole.enrollments.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.enrollments.api.dto.ChangeEnrollmentClassroomRequest;
import com.galactique.gestionecole.enrollments.api.dto.CreateEnrollmentRequest;
import com.galactique.gestionecole.enrollments.api.dto.EnrollmentCreationResponse;
import com.galactique.gestionecole.enrollments.api.dto.EnrollmentResponse;
import com.galactique.gestionecole.enrollments.application.CreateEnrollmentCommand;
import com.galactique.gestionecole.enrollments.application.EnrollmentApplicationService;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentInput;

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
	@PreAuthorize("hasAuthority('can_enrollment_create') and "
		+ "(#request.payments() == null or #request.payments().isEmpty() or hasAuthority('can_payment_record'))")
	@ResponseStatus(HttpStatus.CREATED)
	public EnrollmentCreationResponse createEnrollment(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @Valid @RequestBody CreateEnrollmentRequest request,
		Authentication authentication) {
		List<EnrollmentPaymentInput> payments = request.payments() == null ? List.of() : request.payments().stream()
			.map(payment -> new EnrollmentPaymentInput(payment.feeId(), payment.amount(), payment.paymentMethodCode(), payment.reference())).toList();
		return EnrollmentCreationResponse.from(enrollmentApplicationService.create(establishmentId, academicYearId,
			new CreateEnrollmentCommand(request.studentId(), request.schoolClassId(), request.classroomId(),
				Boolean.TRUE.equals(request.acceptAdvance()), payments), toActor(authentication)));
	}

	@PatchMapping("/{enrollmentId}")
	@PreAuthorize("hasAuthority('can_enrollment_update')")
	public EnrollmentResponse changeClassroom(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID enrollmentId, @Valid @RequestBody ChangeEnrollmentClassroomRequest request,
		Authentication authentication) {
		return EnrollmentResponse.from(enrollmentApplicationService.changeClassroom(establishmentId, academicYearId,
			enrollmentId, request.classroomId(), toActor(authentication)));
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
