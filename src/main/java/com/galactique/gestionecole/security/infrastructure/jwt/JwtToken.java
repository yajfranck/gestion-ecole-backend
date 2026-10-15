package com.galactique.gestionecole.security.infrastructure.jwt;

public record JwtToken(String value, long expiresInSeconds) {
}