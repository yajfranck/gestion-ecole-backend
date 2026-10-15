package com.galactique.gestionecole.enrollments.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, UUID> {
	boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

	List<EnrollmentEntity> findAllByEstablishmentIdAndAcademicYearIdOrderByCreatedAtAsc(
		UUID establishmentId, UUID academicYearId);

	Optional<EnrollmentEntity> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

	Optional<EnrollmentEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
}