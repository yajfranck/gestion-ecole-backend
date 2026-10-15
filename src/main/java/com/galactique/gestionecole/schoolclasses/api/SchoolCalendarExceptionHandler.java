package com.galactique.gestionecole.schoolclasses.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarConflictException;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarNotFoundException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.schoolclasses.api")
public class SchoolCalendarExceptionHandler {
	@ExceptionHandler(AcademicCalendarNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Academic year, period, or establishment not found");
	}

	@ExceptionHandler(AcademicCalendarAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler({AcademicCalendarConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Academic year or period conflicts with existing calendar data");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Academic calendar dates are invalid");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}