package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, UUID> {
	boolean existsByEstablishmentIdAndCodeIgnoreCase(UUID establishmentId, String code);

	boolean existsByEstablishmentIdAndCodeIgnoreCaseAndIdNot(UUID establishmentId, String code, UUID id);

	List<SubjectEntity> findAllByEstablishmentIdAndEnabledTrueOrderByNameAsc(UUID establishmentId);

	Optional<SubjectEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);
}