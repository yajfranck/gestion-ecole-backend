package com.galactique.gestionecole.security.infrastructure.jwt;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public JwtToken issue(Authentication authentication) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		List<String> authorities = authentication.getAuthorities().stream()
			.map(authority -> Objects.requireNonNull(authority).getAuthority())
			.toList();

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(authentication.getName())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim("authorities", authorities)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new JwtToken(value, properties.accessTokenTtl().toSeconds());
	}
}