package com.galactique.gestionecole.positions.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface PositionJpaRepository extends JpaRepository<PositionEntity, UUID> {
	@EntityGraph(attributePaths = "roleCodes")
	List<PositionEntity> findAllByEstablishmentIdOrderByName(UUID establishmentId);

	@EntityGraph(attributePaths = "roleCodes")
	Optional<PositionEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "roleCodes")
	Optional<PositionEntity> findWithRolesByIdAndEstablishmentId(UUID id, UUID establishmentId);

	boolean existsByEstablishmentIdAndNameIgnoreCase(UUID establishmentId, String name);
	boolean existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(UUID establishmentId, String name, UUID id);
}