package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface TuitionFeeJpaRepository extends JpaRepository<TuitionFeeEntity, UUID> {
	@EntityGraph(attributePaths = "schoolClasses")
	List<TuitionFeeEntity> findAllByEstablishmentIdAndAcademicYearIdOrderByCreatedAtAsc(
		UUID establishmentId, UUID academicYearId);
	@EntityGraph(attributePaths = "schoolClasses")
	Optional<TuitionFeeEntity> findByIdAndEstablishmentIdAndAcademicYearId(
		UUID id, UUID establishmentId, UUID academicYearId);
}
