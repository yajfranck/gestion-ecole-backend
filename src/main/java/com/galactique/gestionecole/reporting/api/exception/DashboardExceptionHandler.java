package com.galactique.gestionecole.reporting.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.reporting.application.DashboardAccessDeniedException;
import com.galactique.gestionecole.reporting.application.DashboardNotFoundException;
import com.galactique.gestionecole.reporting.application.DashboardValidationException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.reporting.api.controller")
public class DashboardExceptionHandler {
	@ExceptionHandler(DashboardAccessDeniedException.class)
	ResponseEntity<ProblemDetail> forbidden() {
		return problem(HttpStatus.FORBIDDEN, "Dashboard access requires an active administrator account in the correct establishment");
	}
	@ExceptionHandler(DashboardNotFoundException.class)
	ResponseEntity<ProblemDetail> notFound() {
		return problem(HttpStatus.NOT_FOUND, "Establishment or academic year not found in the caller's scope");
	}
	@ExceptionHandler(DashboardValidationException.class)
	ResponseEntity<ProblemDetail> invalid() {
		return problem(HttpStatus.BAD_REQUEST, "Filters require an establishment dashboard and a date within the selected academic year");
	}
	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}
