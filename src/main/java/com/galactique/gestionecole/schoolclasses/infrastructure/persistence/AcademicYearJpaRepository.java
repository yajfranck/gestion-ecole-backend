package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface AcademicYearJpaRepository extends JpaRepository<AcademicYearEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT y FROM AcademicYearEntity y WHERE y.id = :id AND y.establishmentId = :establishmentId AND y.enabled = true")
	Optional<AcademicYearEntity> findForUpdate(UUID id, UUID establishmentId);
	boolean existsByEstablishmentIdAndNameIgnoreCase(UUID establishmentId, String name);

	boolean existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(UUID establishmentId, String name, UUID id);

	List<AcademicYearEntity> findAllByEstablishmentIdAndEnabledTrueOrderByStartsOnDesc(UUID establishmentId);

	Optional<AcademicYearEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);
}
