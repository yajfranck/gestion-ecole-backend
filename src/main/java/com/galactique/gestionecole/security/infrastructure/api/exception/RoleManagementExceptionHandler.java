package com.galactique.gestionecole.security.infrastructure.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.security.application.InvalidRoleException;
import com.galactique.gestionecole.security.application.RoleAlreadyExistsException;
import com.galactique.gestionecole.security.application.RoleNotFoundException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.security.infrastructure.api.controller")
public class RoleManagementExceptionHandler {
	@ExceptionHandler(RoleAlreadyExistsException.class)
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "A role with this code already exists");
	}

	@ExceptionHandler(RoleNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Role not found");
	}

	@ExceptionHandler({InvalidRoleException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleInvalidRole(RuntimeException exception) {
		String detail = exception instanceof InvalidRoleException
			? exception.getMessage() : "Role violates a persistence constraint";
		return problem(HttpStatus.BAD_REQUEST, detail);
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}