package com.galactique.gestionecole.security.infrastructure.jwt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.security.infrastructure.persistence.RefreshTokenEntity;
import com.galactique.gestionecole.security.infrastructure.persistence.RefreshTokenJpaRepository;
import com.galactique.gestionecole.security.infrastructure.persistence.SecurityPersistenceMapper;
import com.galactique.gestionecole.security.infrastructure.persistence.UserAccountEntity;
import com.galactique.gestionecole.security.infrastructure.persistence.UserAccountJpaRepository;
import com.galactique.gestionecole.security.infrastructure.security.SecurityUserPrincipal;

@Service
public class RefreshTokenService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenJpaRepository refreshTokenJpaRepository;
	private final UserAccountJpaRepository userAccountJpaRepository;
	private final SecurityPersistenceMapper mapper;
	private final JwtTokenService jwtTokenService;
	private final JwtProperties properties;

	public RefreshTokenService(
		RefreshTokenJpaRepository refreshTokenJpaRepository,
		UserAccountJpaRepository userAccountJpaRepository,
		SecurityPersistenceMapper mapper,
		JwtTokenService jwtTokenService,
		JwtProperties properties) {
		this.refreshTokenJpaRepository = refreshTokenJpaRepository;
		this.userAccountJpaRepository = userAccountJpaRepository;
		this.mapper = mapper;
		this.jwtTokenService = jwtTokenService;
		this.properties = properties;
	}

	@Transactional
	public IssuedTokenPair issueFor(Authentication authentication) {
		Instant now = Instant.now();
		refreshTokenJpaRepository.deleteAllByExpiresAtBefore(now);
		UserAccountEntity userAccount = userAccountJpaRepository
			.findByUsernameIgnoreCase(authentication.getName())
			.orElseThrow(InvalidRefreshTokenException::new);
		return issuePair(authentication, userAccount.getId(), UUID.randomUUID(), now);
	}

	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public IssuedTokenPair rotate(String presentedToken) {
		if (presentedToken == null || presentedToken.isBlank() || presentedToken.length() > 100) {
			throw new InvalidRefreshTokenException();
		}

		Instant now = Instant.now();
		RefreshTokenEntity current = refreshTokenJpaRepository
			.findByTokenHashForUpdate(hashToken(presentedToken))
			.orElseThrow(InvalidRefreshTokenException::new);

		if (current.isRevoked()) {
			revokeFamily(current.getFamilyId(), now);
			throw new InvalidRefreshTokenException();
		}
		if (!current.getExpiresAt().isAfter(now)) {
			current.revoke(now);
			throw new InvalidRefreshTokenException();
		}

		UserAccountEntity userAccount = userAccountJpaRepository
			.findByIdWithRoles(current.getUserAccountId())
			.orElseThrow(() -> revokeAndReject(current, now));
		SecurityUserPrincipal principal = new SecurityUserPrincipal(mapper.toDomain(userAccount));
		if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
			revokeFamily(current.getFamilyId(), now);
			throw new InvalidRefreshTokenException();
		}

		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
			principal,
			null,
			principal.getAuthorities());
		current.revoke(now);
		IssuedTokenPair tokenPair = issuePair(authentication, userAccount.getId(), current.getFamilyId(), now);
		refreshTokenJpaRepository.deleteAllByExpiresAtBefore(now);
		return tokenPair;
	}

	private IssuedTokenPair issuePair(Authentication authentication, UUID userAccountId, UUID familyId, Instant now) {
		String rawToken = generateToken();
		Instant expiresAt = now.plus(properties.refreshTokenTtl());
		refreshTokenJpaRepository.save(new RefreshTokenEntity(
			hashToken(rawToken), userAccountId, familyId, now, expiresAt));
		return new IssuedTokenPair(
			jwtTokenService.issue(authentication),
			rawToken,
			properties.refreshTokenTtl().toSeconds());
	}

	private String generateToken() {
		byte[] tokenBytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	private String hashToken(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private void revokeFamily(UUID familyId, Instant now) {
		refreshTokenJpaRepository.revokeActiveByFamilyId(familyId, now);
	}

	private InvalidRefreshTokenException revokeAndReject(RefreshTokenEntity token, Instant now) {
		revokeFamily(token.getFamilyId(), now);
		return new InvalidRefreshTokenException();
	}
}