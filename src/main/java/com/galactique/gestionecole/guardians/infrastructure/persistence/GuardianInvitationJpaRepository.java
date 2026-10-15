package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface GuardianInvitationJpaRepository extends JpaRepository<GuardianInvitationEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invitation from GuardianInvitationEntity invitation where invitation.tokenHash = :tokenHash")
	Optional<GuardianInvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}