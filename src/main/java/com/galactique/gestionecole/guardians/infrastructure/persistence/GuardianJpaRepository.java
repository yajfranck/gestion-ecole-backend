package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface GuardianJpaRepository extends JpaRepository<GuardianEntity, UUID> {
	Optional<GuardianEntity> findByEmailIgnoreCase(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select guardian from GuardianEntity guardian where guardian.id = :id")
	Optional<GuardianEntity> findByIdForUpdate(@Param("id") UUID id);
}