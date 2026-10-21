package com.galactique.gestionecole.schoolclasses.application;

public class SchoolClassBulkConflictException extends RuntimeException {
	public SchoolClassBulkConflictException(int index, String detail) {
		super("classes[" + index + "]: " + detail);
	}
}