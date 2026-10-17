package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface SchoolClassJpaRepository extends JpaRepository<SchoolClassEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM SchoolClassEntity c WHERE c.id = :id AND c.establishmentId = :establishmentId "
		+ "AND c.academicYearId = :academicYearId AND c.enabled = true")
	Optional<SchoolClassEntity> findForUpdate(UUID id, UUID establishmentId, UUID academicYearId);
	boolean existsByAcademicYearIdAndCodeIgnoreCase(UUID academicYearId, String code);

	boolean existsByAcademicYearIdAndCodeIgnoreCaseAndIdNot(UUID academicYearId, String code, UUID id);

	List<SchoolClassEntity> findAllByEstablishmentIdAndAcademicYearIdAndEnabledTrueOrderByNameAsc(
		UUID establishmentId, UUID academicYearId);

	Optional<SchoolClassEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);

	Optional<SchoolClassEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
		UUID id, UUID establishmentId, UUID academicYearId);

	Optional<SchoolClassEntity> findByIdAndEstablishmentIdAndAcademicYearId(
		UUID id, UUID establishmentId, UUID academicYearId);
}
