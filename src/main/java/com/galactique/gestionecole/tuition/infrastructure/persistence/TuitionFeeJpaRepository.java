package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TuitionFeeJpaRepository extends JpaRepository<TuitionFeeEntity, UUID> {
	List<TuitionFeeEntity> findAllByEstablishmentIdAndAcademicYearIdOrderByCreatedAtAsc(
		UUID establishmentId, UUID academicYearId);
	Optional<TuitionFeeEntity> findByIdAndEstablishmentIdAndAcademicYearId(
		UUID id, UUID establishmentId, UUID academicYearId);
}