package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeLevelJpaRepository extends JpaRepository<GradeLevelEntity, UUID> {
	boolean existsByEstablishmentIdAndCodeIgnoreCase(UUID establishmentId, String code);

	boolean existsByEstablishmentIdAndCodeIgnoreCaseAndIdNot(UUID establishmentId, String code, UUID id);

	boolean existsByEstablishmentIdAndNameIgnoreCase(UUID establishmentId, String name);

	boolean existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(UUID establishmentId, String name, UUID id);

	List<GradeLevelEntity> findAllByEstablishmentIdAndEnabledTrueOrderByDisplayOrderAscNameAsc(UUID establishmentId);

	Optional<GradeLevelEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);
}