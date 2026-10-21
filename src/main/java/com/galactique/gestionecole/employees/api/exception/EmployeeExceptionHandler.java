package com.galactique.gestionecole.employees.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeConflictException;
import com.galactique.gestionecole.employees.application.EmployeeNotFoundException;
import com.galactique.gestionecole.employees.application.InvalidEmployeeInvitationException;
import com.galactique.gestionecole.employees.application.InvalidEmployeeRolesException;
import com.galactique.gestionecole.establishments.application.EstablishmentConflictException;
import com.galactique.gestionecole.establishments.application.EstablishmentNotFoundException;

@RestControllerAdvice(basePackages = {
	"com.galactique.gestionecole.employees.api",
	"com.galactique.gestionecole.establishments.api",
	"com.galactique.gestionecole.positions.api"
})
public class EmployeeExceptionHandler {
	@ExceptionHandler({EmployeeNotFoundException.class, EstablishmentNotFoundException.class})
	ResponseEntity<ProblemDetail> handleNotFound() {
		return problem(HttpStatus.NOT_FOUND, "Employee member or establishment not found");
	}

	@ExceptionHandler(EmployeeAccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied() {
		return problem(HttpStatus.FORBIDDEN, "Operation is outside the caller's establishment scope");
	}

	@ExceptionHandler(EmployeeConflictException.class)
	ResponseEntity<ProblemDetail> handleConflict() {
		return problem(HttpStatus.CONFLICT, "Employee email or employee number already exists");
	}

	@ExceptionHandler(EstablishmentConflictException.class)
	ResponseEntity<ProblemDetail> handleEstablishmentConflict() {
		return problem(HttpStatus.CONFLICT, "Establishment code already exists");
	}

	@ExceptionHandler(InvalidEmployeeRolesException.class)
	ResponseEntity<ProblemDetail> handleInvalidRoles() {
		return problem(HttpStatus.BAD_REQUEST, "Role codes must refer to enabled roles assignable to positions");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest() {
		return problem(HttpStatus.BAD_REQUEST, "At least one valid employee field must be provided");
	}

	@ExceptionHandler(InvalidEmployeeInvitationException.class)
	ResponseEntity<ProblemDetail> handleInvalidInvitation() {
		return problem(HttpStatus.UNAUTHORIZED, "Invitation token is invalid, expired, or already used");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> handleDataConflict() {
		return problem(HttpStatus.CONFLICT, "A record with the same unique value already exists");
	}

	@ExceptionHandler(MailException.class)
	ResponseEntity<ProblemDetail> handleMailFailure() {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Invitation email could not be sent");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}
