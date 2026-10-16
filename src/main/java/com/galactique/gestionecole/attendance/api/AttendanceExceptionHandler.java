package com.galactique.gestionecole.attendance.api;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger logger = LoggerFactory.getLogger(AttendanceExceptionHandler.class);
	@ExceptionHandler(AttendanceNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "The attendance context or enrollment was not found");
	}

	@ExceptionHandler(AttendanceAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Attendance access is outside the caller's establishment");
	}

	@ExceptionHandler({AttendanceConflictException.class, DataIntegrityViolationException.class})
	ResponseEntity<ProblemDetail> handleConflict(Exception exception, HttpServletRequest request) {
		Throwable cause = exception;
		SQLException sqlException = null;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
			if (cause instanceof SQLException sql) {
				sqlException = sql;
			}
		}
		// Keep the concise diagnostic for all conflicts; include full causes for persistence failures.
		logger.warn("Attendance conflict: method={}, path={}, type={}, causeType={}, sqlState={}, sqlErrorCode={}",
			request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName(),
			cause.getClass().getSimpleName(), sqlException == null ? null : sqlException.getSQLState(),
			sqlException == null ? null : sqlException.getErrorCode());
		if (exception instanceof DataIntegrityViolationException || exception.getCause() != null) {
			logger.error("Attendance persistence failure: method={}, path={}",
				request.getMethod(), request.getRequestURI(), exception);
			// JDBC can retain additional errors outside the regular cause chain.
			Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
			for (Throwable current = exception; current != null && visited.add(current); current = current.getCause()) {
				if (current instanceof SQLException sql) {
					Set<SQLException> linked = Collections.newSetFromMap(new IdentityHashMap<>());
					linked.add(sql);
					for (SQLException next = sql.getNextException(); next != null && linked.add(next);
						next = next.getNextException()) {
						logger.error("Attendance linked SQL exception: sqlState={}, sqlErrorCode={}",
							next.getSQLState(), next.getErrorCode(), next);
					}
				}
			}
		}
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