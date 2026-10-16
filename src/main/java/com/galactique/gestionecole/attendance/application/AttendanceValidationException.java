package com.galactique.gestionecole.attendance.application;

public class AttendanceValidationException extends RuntimeException {
	public AttendanceValidationException(String message) {
		super(message);
	}
}