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
	public Enrollment save(UUID establishmentId, UUID studentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
		boolean advanceAccepted, String paymentDecisionBy) {
		return jpaRepository.saveAndFlush(
			new EnrollmentEntity(establishmentId, studentId, academicYearId, schoolClassId, classroomId,
				advanceAccepted, paymentDecisionBy))
			.toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<Enrollment> findById(UUID establishmentId, UUID academicYearId, UUID enrollmentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndAcademicYearId(enrollmentId, establishmentId, academicYearId)
			.map(EnrollmentEntity::toDomain);
	}

	@Override
	@Transactional
	public Enrollment updateClassroom(Enrollment enrollment, UUID classroomId) {
		var entity = jpaRepository.findByIdAndEstablishmentIdAndAcademicYearId(enrollment.id(),
			enrollment.establishmentId(), enrollment.academicYearId()).orElseThrow();
		entity.changeClassroom(classroomId);
		return jpaRepository.saveAndFlush(entity).toDomain();
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

	@Override
	@Transactional(readOnly = true)
	public List<Enrollment> findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassId(
		UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		return jpaRepository.findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassIdOrderByCreatedAtAsc(
			establishmentId, academicYearId, schoolClassId).stream().map(EnrollmentEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<Enrollment> findByEstablishmentIdAndAcademicYearIdAndStudentId(
		UUID establishmentId, UUID academicYearId, UUID studentId) {
		return jpaRepository.findByEstablishmentIdAndAcademicYearIdAndStudentId(
			establishmentId, academicYearId, studentId).map(EnrollmentEntity::toDomain);
	}
}
