package com.galactique.gestionecole.enrollments.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.enrollments.application.EnrollmentAccessDeniedException;
import com.galactique.gestionecole.enrollments.application.EnrollmentConflictException;
import com.galactique.gestionecole.enrollments.application.EnrollmentNotFoundException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.enrollments.api")
public class EnrollmentExceptionHandler {
	@ExceptionHandler(EnrollmentNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Student, class, academic year, or establishment not found");
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
}