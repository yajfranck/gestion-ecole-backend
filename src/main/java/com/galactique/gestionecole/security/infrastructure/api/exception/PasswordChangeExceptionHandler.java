package com.galactique.gestionecole.security.infrastructure.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.security.application.IncorrectCurrentPasswordException;
import com.galactique.gestionecole.security.application.InvalidNewPasswordException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.security.infrastructure.api.controller")
public class PasswordChangeExceptionHandler {
	@ExceptionHandler(IncorrectCurrentPasswordException.class)
	ResponseEntity<ProblemDetail> handleIncorrectCurrentPassword() {
		return problem(HttpStatus.FORBIDDEN, "Current password is incorrect");
	}

	@ExceptionHandler(InvalidNewPasswordException.class)
	ResponseEntity<ProblemDetail> handleInvalidNewPassword(InvalidNewPasswordException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}