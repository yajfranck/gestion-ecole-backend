package com.galactique.gestionecole.security.application;

public class InvalidRoleException extends RuntimeException {
	public InvalidRoleException(String message) {
		super(message);
	}
}