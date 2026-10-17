package com.galactique.gestionecole.schoolclasses.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogConflictException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.schoolclasses.api")
public class SchoolCatalogExceptionHandler {
	@ExceptionHandler(SchoolCatalogNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "School catalog record or establishment not found");
	}

	@ExceptionHandler(SchoolCatalogAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler({SchoolCatalogConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "School catalog code or association already exists");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest() {
		return problem(HttpStatus.BAD_REQUEST, "School catalog data is invalid");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}
