package com.galactique.gestionecole.security.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select token from RefreshTokenEntity token where token.tokenHash = :tokenHash")
	Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Modifying
	@Query("update RefreshTokenEntity token set token.revokedAt = :revokedAt "
		+ "where token.familyId = :familyId and token.revokedAt is null")
	int revokeActiveByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

	@Modifying
	@Query("update RefreshTokenEntity token set token.revokedAt = :revokedAt "
		+ "where token.userAccountId = :userAccountId and token.revokedAt is null")
	int revokeActiveByUserAccountId(@Param("userAccountId") UUID userAccountId,
		@Param("revokedAt") Instant revokedAt);

	long deleteAllByExpiresAtBefore(Instant expiresAt);
}