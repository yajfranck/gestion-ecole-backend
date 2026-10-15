package com.galactique.gestionecole.security.application;

public class InvalidNewPasswordException extends RuntimeException {
	public InvalidNewPasswordException(String message) {
		super(message);
	}
}