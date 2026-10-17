package com.galactique.gestionecole.timetables.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.galactique.gestionecole.timetables.application.TimetableAccessDeniedException;
import com.galactique.gestionecole.timetables.application.TimetableConflictException;
import com.galactique.gestionecole.timetables.application.TimetableNotFoundException;
import com.galactique.gestionecole.timetables.application.TimetableValidationException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.timetables.api.controller")
public class TimetableExceptionHandler {
	@ExceptionHandler(TimetableAccessDeniedException.class)
	ResponseEntity<ProblemDetail> forbidden() { return problem(HttpStatus.FORBIDDEN, "Active staff account, permission and SCHOOL_MANAGEMENT entitlement required"); }
	@ExceptionHandler(TimetableNotFoundException.class)
	ResponseEntity<ProblemDetail> notFound() { return problem(HttpStatus.NOT_FOUND, "Timetable resource or active assignment not found in this establishment"); }
	@ExceptionHandler(TimetableValidationException.class)
	ResponseEntity<ProblemDetail> invalid(TimetableValidationException exception) { return problem(HttpStatus.BAD_REQUEST, exception.getMessage()); }
	@ExceptionHandler(TimetableConflictException.class)
	ResponseEntity<ProblemDetail> conflict(TimetableConflictException exception) {
		var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Overlapping classroom, teacher or physical room timetable");
		detail.setProperty("conflictingEntryId", exception.conflictingEntryId());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
	}
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> integrity() { return problem(HttpStatus.CONFLICT, "Timetable conflicts with an existing resource"); }
	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, message));
	}
}
