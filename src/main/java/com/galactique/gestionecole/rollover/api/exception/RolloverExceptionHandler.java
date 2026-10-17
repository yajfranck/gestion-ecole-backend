package com.galactique.gestionecole.rollover.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.galactique.gestionecole.rollover.application.RolloverException;

@RestControllerAdvice(basePackages = "com.galactique.gestionecole.rollover.api.controller")
public class RolloverExceptionHandler {
    @ExceptionHandler(RolloverException.class)
    ResponseEntity<ProblemDetail> handle(RolloverException exception) {
        HttpStatus status = switch (exception.reason()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "A destination code, name or configuration is already reserved; no changes were saved"));
    }
}
