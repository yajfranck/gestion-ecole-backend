package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicPeriodJpaRepository extends JpaRepository<AcademicPeriodEntity, UUID> {
	boolean existsByAcademicYearIdAndNameIgnoreCase(UUID academicYearId, String name);

	boolean existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(UUID academicYearId, String name, UUID id);

	List<AcademicPeriodEntity> findAllByAcademicYearIdOrderByStartsOnAsc(UUID academicYearId);

	List<AcademicPeriodEntity> findAllByAcademicYearIdAndEnabledTrueOrderByStartsOnAsc(UUID academicYearId);

	Optional<AcademicPeriodEntity> findByIdAndAcademicYearIdAndEnabledTrue(UUID id, UUID academicYearId);
}