package com.galactique.gestionecole.schoolclasses.application;

public record CreateSubjectCommand(String code, String name) {
	public CreateSubjectCommand(String name) { this(null, name); }
}
