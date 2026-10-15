package com.galactique.gestionecole.enrollments.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.enrollments.domain.EnrollmentRepository;

@Repository
public class JpaEnrollmentRepositoryAdapter implements EnrollmentRepository {
	private final EnrollmentJpaRepository jpaRepository;

	public JpaEnrollmentRepositoryAdapter(EnrollmentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Enrollment save(UUID establishmentId, UUID studentId, UUID academicYearId, UUID schoolClassId) {
		return jpaRepository.saveAndFlush(
			new EnrollmentEntity(establishmentId, studentId, academicYearId, schoolClassId))
			.toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId) {
		return jpaRepository.existsByStudentIdAndAcademicYearId(studentId, academicYearId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Enrollment> findAllByEstablishmentIdAndAcademicYearId(UUID establishmentId, UUID academicYearId) {
		return jpaRepository.findAllByEstablishmentIdAndAcademicYearIdOrderByCreatedAtAsc(
			establishmentId, academicYearId).stream().map(EnrollmentEntity::toDomain).toList();
	}
}