package com.galactique.gestionecole.attendance.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.attendance.application.AttendanceAccessDeniedException;
import com.galactique.gestionecole.attendance.application.AttendanceConflictException;
import com.galactique.gestionecole.attendance.application.AttendanceNotFoundException;
import com.galactique.gestionecole.attendance.application.AttendanceValidationException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.attendance.api")
public class AttendanceExceptionHandler {
	@ExceptionHandler(AttendanceNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "The attendance context or enrollment was not found");
	}

	@ExceptionHandler(AttendanceAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Attendance access is outside the caller's establishment");
	}

	@ExceptionHandler({AttendanceConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Attendance was concurrently modified");
	}

	@ExceptionHandler({AttendanceValidationException.class, IllegalArgumentException.class})
	ResponseEntity<ProblemDetail> handleInvalidRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Attendance request is invalid");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}