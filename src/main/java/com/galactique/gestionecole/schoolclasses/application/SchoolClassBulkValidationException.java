package com.galactique.gestionecole.schoolclasses.application;

public class SchoolClassBulkValidationException extends RuntimeException {
	public SchoolClassBulkValidationException(int index, String detail) {
		super("classes[" + index + "]: " + detail);
	}
}