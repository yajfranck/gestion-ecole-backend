package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassEntity, UUID> {
	boolean existsByAcademicYearIdAndCodeIgnoreCase(UUID academicYearId, String code);

	boolean existsByAcademicYearIdAndCodeIgnoreCaseAndIdNot(UUID academicYearId, String code, UUID id);

	boolean existsByAcademicYearIdAndGradeLevelIdAndNameIgnoreCase(
		UUID academicYearId, UUID gradeLevelId, String name);

	List<SchoolClassEntity> findAllByEstablishmentIdAndAcademicYearIdAndEnabledTrueOrderByNameAsc(
		UUID establishmentId, UUID academicYearId);

	boolean existsByEstablishmentIdAndGradeLevelIdAndEnabledTrue(UUID establishmentId, UUID gradeLevelId);

	Optional<SchoolClassEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);

	Optional<SchoolClassEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
		UUID id, UUID establishmentId, UUID academicYearId);
}