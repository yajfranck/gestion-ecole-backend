package com.galactique.gestionecole.security.infrastructure.jwt;

public record IssuedTokenPair(JwtToken accessToken, String refreshToken, long refreshExpiresInSeconds) {
}