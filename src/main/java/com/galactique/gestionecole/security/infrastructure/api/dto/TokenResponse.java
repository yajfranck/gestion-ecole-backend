package com.galactique.gestionecole.security.infrastructure.api.dto;

public record TokenResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	String refreshToken,
	long refreshExpiresIn) {
}
