package com.galactique.gestionecole.schoolclasses.application;

public record UpdateSubjectCommand(String code, String name) {
	public UpdateSubjectCommand(String name) { this(null, name); }
}
