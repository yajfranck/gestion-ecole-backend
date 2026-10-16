package com.galactique.gestionecole.learning.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galactique.gestionecole.learning.domain.LearningContentStatus;
import com.galactique.gestionecole.learning.domain.LearningContentType;

public interface LearningContentJpaRepository extends JpaRepository<LearningContentEntity, UUID> {
	@Query("select content from LearningContentEntity content where content.establishmentId = :establishmentId "
		+ "and content.academicYearId = :academicYearId and content.schoolClassId = :schoolClassId "
		+ "and content.status <> :archived and (:type is null or content.type = :type) "
		+ "and (:status is null or content.status = :status) order by content.createdAt desc, content.id desc")
	Page<LearningContentEntity> findPage(@Param("establishmentId") UUID establishmentId,
		@Param("academicYearId") UUID academicYearId, @Param("schoolClassId") UUID schoolClassId,
		@Param("type") LearningContentType type, @Param("status") LearningContentStatus status,
		@Param("archived") LearningContentStatus archived, Pageable pageable);

	Optional<LearningContentEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassId(
		UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId);
}
