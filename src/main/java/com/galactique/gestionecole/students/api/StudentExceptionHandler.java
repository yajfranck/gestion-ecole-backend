package com.galactique.gestionecole.students.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.students.application.StudentAccessDeniedException;
import com.galactique.gestionecole.students.application.StudentConflictException;
import com.galactique.gestionecole.students.application.StudentNotFoundException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.students.api")
public class StudentExceptionHandler {
	@ExceptionHandler(StudentNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Student or establishment not found");
	}

	@ExceptionHandler(StudentAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler({StudentConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Student number already exists in this establishment");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest() {
		return problem(HttpStatus.BAD_REQUEST, "At least one valid student field must be provided");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}