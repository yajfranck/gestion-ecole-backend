package com.galactique.gestionecole.attendance.application;

public class AttendanceConflictException extends RuntimeException {
	public AttendanceConflictException() {
		super();
	}

	public AttendanceConflictException(Throwable cause) {
		super("Attendance persistence conflict", cause);
	}
}