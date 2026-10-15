package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassSubjectJpaRepository extends JpaRepository<SchoolClassSubjectEntity, UUID> {
	Optional<SchoolClassSubjectEntity> findByEstablishmentIdAndSchoolClassIdAndSubjectId(
		UUID establishmentId, UUID schoolClassId, UUID subjectId);

	boolean existsByEstablishmentIdAndSubjectIdAndEnabledTrue(UUID establishmentId, UUID subjectId);

	List<SchoolClassSubjectEntity> findAllByEstablishmentIdAndSchoolClassIdAndEnabledTrueOrderByCreatedAtAsc(
		UUID establishmentId, UUID schoolClassId);

	Optional<SchoolClassSubjectEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);
}