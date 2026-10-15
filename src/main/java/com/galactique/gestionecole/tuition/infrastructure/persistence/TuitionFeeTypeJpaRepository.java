package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TuitionFeeTypeJpaRepository extends JpaRepository<TuitionFeeTypeEntity, UUID> {
	List<TuitionFeeTypeEntity> findAllByEstablishmentIdAndEnabledTrueOrderByNameAsc(UUID establishmentId);
	Optional<TuitionFeeTypeEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
	boolean existsByEstablishmentIdAndCodeIgnoreCase(UUID establishmentId, String code);
	boolean existsByEstablishmentIdAndNameIgnoreCase(UUID establishmentId, String name);
	boolean existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(UUID establishmentId, String name, UUID id);
}