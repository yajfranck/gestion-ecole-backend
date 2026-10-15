package com.galactique.gestionecole.guardians.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.guardians.application.GuardianAccessDeniedException;
import com.galactique.gestionecole.guardians.application.GuardianConflictException;
import com.galactique.gestionecole.guardians.application.GuardianInvitationConflictException;
import com.galactique.gestionecole.guardians.application.GuardianNotFoundException;
import com.galactique.gestionecole.guardians.application.InvalidGuardianInvitationException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.guardians.api.controller")
public class GuardianExceptionHandler {
	@ExceptionHandler(GuardianNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Guardian or student not found in the establishment");
	}

	@ExceptionHandler(GuardianAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler({GuardianConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Guardian already exists or is already linked to this student");
	}

	@ExceptionHandler(InvalidGuardianInvitationException.class)
	ResponseEntity<ProblemDetail> handleInvalidInvitation() {
		return problem(HttpStatus.UNAUTHORIZED, "Guardian invitation is invalid, expired, or already used");
	}

	@ExceptionHandler(GuardianInvitationConflictException.class)
	ResponseEntity<ProblemDetail> handleInvitationConflict() {
		return problem(HttpStatus.CONFLICT, "An account already exists for this email address");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}