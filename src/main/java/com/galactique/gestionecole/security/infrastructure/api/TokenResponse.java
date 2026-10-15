package com.galactique.gestionecole.security.infrastructure.api;

public record TokenResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	String refreshToken,
	long refreshExpiresIn) {
}