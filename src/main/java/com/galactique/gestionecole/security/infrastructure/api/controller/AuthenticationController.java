package com.galactique.gestionecole.security.infrastructure.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.security.infrastructure.api.dto.LoginRequest;
import com.galactique.gestionecole.security.infrastructure.api.dto.RefreshTokenRequest;
import com.galactique.gestionecole.security.infrastructure.api.dto.TokenResponse;
import com.galactique.gestionecole.security.infrastructure.jwt.InvalidRefreshTokenException;
import com.galactique.gestionecole.security.infrastructure.jwt.IssuedTokenPair;
import com.galactique.gestionecole.security.infrastructure.jwt.JwtToken;
import com.galactique.gestionecole.security.infrastructure.jwt.RefreshTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AuthenticationController {
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

	private final AuthenticationManager authenticationManager;
	private final RefreshTokenService refreshTokenService;

	public AuthenticationController(AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService) {
		this.authenticationManager = authenticationManager;
		this.refreshTokenService = refreshTokenService;
	}

	/** Authentifie un utilisateur et émet une paire de jetons. */
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		String safeLogin = request.login().replaceAll("\\p{Cntrl}", "?");
		logger.debug("Login attempt for identifier '{}'", safeLogin);

		try {
			Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.login(), request.password()));
			IssuedTokenPair tokenPair = refreshTokenService.issueFor(authentication);
			logger.debug("Login succeeded for identifier '{}'", safeLogin);
			return ResponseEntity.ok(toResponse(tokenPair));
		} catch (AuthenticationException exception) {
			logger.warn("Login rejected ({})", exception.getClass().getSimpleName());
			logger.debug("Login rejected for identifier '{}'", safeLogin);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	/** Renouvelle les jetons et invalide le refresh token précédent. */
	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		try {
			return ResponseEntity.ok(toResponse(refreshTokenService.rotate(request.refreshToken())));
		} catch (InvalidRefreshTokenException exception) {
			logger.warn("Refresh token rejected");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	private TokenResponse toResponse(IssuedTokenPair tokenPair) {
		JwtToken accessToken = tokenPair.accessToken();
		return new TokenResponse(
			accessToken.value(),
			"Bearer",
			accessToken.expiresInSeconds(),
			tokenPair.refreshToken(),
			tokenPair.refreshExpiresInSeconds());
	}
}
