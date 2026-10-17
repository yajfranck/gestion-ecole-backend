package com.galactique.gestionecole.enrollments.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, UUID> {
	Optional<EnrollmentEntity> findByIdAndEstablishmentIdAndAcademicYearId(UUID id, UUID establishmentId, UUID academicYearId);
	boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

	List<EnrollmentEntity> findAllByEstablishmentIdAndAcademicYearIdOrderByCreatedAtAsc(
		UUID establishmentId, UUID academicYearId);

	List<EnrollmentEntity> findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassIdOrderByCreatedAtAsc(
		UUID establishmentId, UUID academicYearId, UUID schoolClassId);

	Optional<EnrollmentEntity> findByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

	Optional<EnrollmentEntity> findByEstablishmentIdAndAcademicYearIdAndStudentId(
		UUID establishmentId, UUID academicYearId, UUID studentId);

	Optional<EnrollmentEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
}
