package com.galactique.gestionecole.enrollments.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.enrollments.application.EnrollmentAccessDeniedException;
import com.galactique.gestionecole.enrollments.application.EnrollmentConflictException;
import com.galactique.gestionecole.enrollments.application.EnrollmentNotFoundException;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentConflictException;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentRequiredException;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentValidationException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.enrollments.api")
public class EnrollmentExceptionHandler {
	@ExceptionHandler(EnrollmentNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Student, enrollment, classroom, class, academic year, or establishment not found");
	}

	@ExceptionHandler(EnrollmentAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler({EnrollmentConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Student already has an enrollment for this academic year");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Invalid enrollment data; select classroomId when the class has multiple or no active classrooms");
	}

	@ExceptionHandler(EnrollmentPaymentRequiredException.class)
	ResponseEntity<ProblemDetail> handlePaymentRequired(EnrollmentPaymentRequiredException exception) {
		var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		detail.setProperty("code", "ENROLLMENT_PAYMENT_REQUIRED");
		detail.setProperty("unpaidFeeIds", exception.feeIds());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
	}

	@ExceptionHandler(EnrollmentPaymentConflictException.class)
	ResponseEntity<ProblemDetail> handlePaymentConflict() {
		return problem(HttpStatus.CONFLICT, "Payment exceeds the outstanding balance or conflicts with an existing payment");
	}

	@ExceptionHandler(EnrollmentPaymentValidationException.class)
	ResponseEntity<ProblemDetail> handleInvalidPayment(EnrollmentPaymentValidationException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
