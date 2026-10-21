package com.galactique.gestionecole.learning.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.learning.application.LearningContentAccessDeniedException;
import com.galactique.gestionecole.learning.application.LearningContentConflictException;
import com.galactique.gestionecole.learning.application.LearningContentNotFoundException;
import com.galactique.gestionecole.learning.application.LearningContentValidationException;
import com.galactique.gestionecole.learning.infrastructure.storage.LearningStorageException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.learning.api")
public class LearningContentExceptionHandler {
	@ExceptionHandler(LearningContentNotFoundException.class)
	public ResponseEntity<ProblemDetail> notFound() {
		return problem(HttpStatus.NOT_FOUND, "Learning content or attachment was not found");
	}

	@ExceptionHandler(LearningContentAccessDeniedException.class)
	public ResponseEntity<ProblemDetail> forbidden() {
		return problem(HttpStatus.FORBIDDEN, "Learning content access is outside the caller's scope");
	}

	@ExceptionHandler(LearningContentConflictException.class)
	public ResponseEntity<ProblemDetail> conflict() {
		return problem(HttpStatus.CONFLICT, "Learning content cannot be changed in its current state");
	}

	@ExceptionHandler(LearningContentValidationException.class)
	public ResponseEntity<ProblemDetail> badRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Learning content request is invalid");
	}

	@ExceptionHandler(LearningStorageException.class)
	public ResponseEntity<ProblemDetail> storageFailure() {
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Learning attachment storage is unavailable");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}
