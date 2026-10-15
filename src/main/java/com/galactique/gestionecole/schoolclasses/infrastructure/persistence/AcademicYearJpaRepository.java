package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearJpaRepository extends JpaRepository<AcademicYearEntity, UUID> {
	boolean existsByEstablishmentIdAndNameIgnoreCase(UUID establishmentId, String name);

	boolean existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(UUID establishmentId, String name, UUID id);

	List<AcademicYearEntity> findAllByEstablishmentIdAndEnabledTrueOrderByStartsOnDesc(UUID establishmentId);

	Optional<AcademicYearEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);
}