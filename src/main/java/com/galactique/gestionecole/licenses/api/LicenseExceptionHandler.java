package com.galactique.gestionecole.licenses.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.establishments.application.EstablishmentNotFoundException;
import com.galactique.gestionecole.licenses.application.LicenseConflictException;
import com.galactique.gestionecole.licenses.application.LicenseAccessDeniedException;
import com.galactique.gestionecole.licenses.application.LicenseNotFoundException;
import com.galactique.gestionecole.licenses.application.LicenseValidationException;

@RestControllerAdvice(basePackageClasses = LicenseController.class)
public class LicenseExceptionHandler {
	@ExceptionHandler({LicenseNotFoundException.class, EstablishmentNotFoundException.class})
	public ProblemDetail notFound() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "License or establishment was not found");
	}

	@ExceptionHandler(LicenseConflictException.class)
	public ProblemDetail conflict() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "License operation conflicts with the current state");
	}

	@ExceptionHandler(LicenseAccessDeniedException.class)
	public ProblemDetail forbidden() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "License access is outside the caller's establishment");
	}

	@ExceptionHandler(LicenseValidationException.class)
	public ProblemDetail validation() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "License request is invalid");
	}
}
