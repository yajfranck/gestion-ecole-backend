package com.galactique.gestionecole.schoolclasses.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galactique.gestionecole.schoolclasses.application.SchoolClassRequiresClassroomException;

@RestControllerAdvice(basePackages = {
	"com.galactique.gestionecole.schoolclasses.api",
	"com.galactique.gestionecole.enrollments.api",
	"com.galactique.gestionecole.attendance.api.controller",
	"com.galactique.gestionecole.timetables.api.controller",
	"com.galactique.gestionecole.learning.api"
})
public class SchoolClassRequiresClassroomExceptionHandler {
	@ExceptionHandler(SchoolClassRequiresClassroomException.class)
	ResponseEntity<ProblemDetail> handleRequiresClassroom(SchoolClassRequiresClassroomException exception) {
		return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			exception.getMessage()));
	}
}