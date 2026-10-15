package com.galactique.gestionecole.security.infrastructure.jwt;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
	public JwtProperties {
		Objects.requireNonNull(secret, "JWT secret must be configured");
		Objects.requireNonNull(issuer, "JWT issuer must be configured");
		Objects.requireNonNull(accessTokenTtl, "JWT access token lifetime must be configured");
		Objects.requireNonNull(refreshTokenTtl, "JWT refresh token lifetime must be configured");
		if (secret.isBlank() || issuer.isBlank() || accessTokenTtl.isNegative() || accessTokenTtl.isZero()
			|| refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
			throw new IllegalArgumentException("JWT secret, issuer, and positive token lifetimes must be configured");
		}
	}
}