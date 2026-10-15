package com.galactique.gestionecole.security.infrastructure.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.galactique.gestionecole.security.infrastructure.jwt.JwtToken;
import com.galactique.gestionecole.security.infrastructure.jwt.IssuedTokenPair;
import com.galactique.gestionecole.security.infrastructure.jwt.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTests {
	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private AuthenticationController authenticationController;

	@Test
	void returnsUnauthorizedWhenCredentialsAreInvalid() {
		when(authenticationManager.authenticate(any(Authentication.class)))
			.thenThrow(new BadCredentialsException("Invalid credentials"));

		var response = authenticationController.login(new LoginRequest("teacher@example.org", "wrong-password"));

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		verifyNoInteractions(refreshTokenService);
	}

	@Test
	void returnsBearerTokenWhenCredentialsAreValid() {
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
			"teacher@example.org",
			null,
			List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
		when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
		when(refreshTokenService.issueFor(authentication))
			.thenReturn(new IssuedTokenPair(new JwtToken("signed-token", 900), "refresh-token", 2_592_000));

		var response = authenticationController.login(new LoginRequest("teacher@example.org", "password"));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("signed-token", response.getBody().accessToken());
		assertEquals("Bearer", response.getBody().tokenType());
		assertEquals(900, response.getBody().expiresIn());
		assertEquals("refresh-token", response.getBody().refreshToken());
		assertEquals(2_592_000, response.getBody().refreshExpiresIn());
	}
}