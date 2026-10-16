package com.galactique.gestionecole.establishments.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EstablishmentJpaRepository extends JpaRepository<EstablishmentEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select establishment from EstablishmentEntity establishment where establishment.id = :id")
	java.util.Optional<EstablishmentEntity> findByIdForUpdate(@Param("id") UUID id);
}