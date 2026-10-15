package com.galactique.gestionecole.tuition.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.tuition.application.TuitionAccessDeniedException;
import com.galactique.gestionecole.tuition.application.TuitionConflictException;
import com.galactique.gestionecole.tuition.application.TuitionNotFoundException;
import com.galactique.gestionecole.tuition.application.TuitionValidationException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.tuition.api.controller")
public class TuitionExceptionHandler {
	@ExceptionHandler(TuitionNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Tuition resource not found in the establishment");
	}

	@ExceptionHandler(TuitionAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler({TuitionConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Tuition operation conflicts with an existing balance or record");
	}

	@ExceptionHandler(TuitionValidationException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest(TuitionValidationException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}