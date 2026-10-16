package com.galactique.gestionecole.students.infrastructure.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.students.domain.StudentRepository;

@Repository
public class JpaStudentRepositoryAdapter implements StudentRepository {
	private final StudentJpaRepository jpaRepository;

	public JpaStudentRepositoryAdapter(StudentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Student save(UUID establishmentId, String firstName, String lastName) {
		String studentNumber = nextStudentNumber(establishmentId);
		return toDomain(jpaRepository.saveAndFlush(
			new StudentEntity(establishmentId, studentNumber, firstName, lastName)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Student> findAllByEstablishmentId(UUID establishmentId) {
		return jpaRepository.findAllByEstablishmentIdAndEnabledTrueOrderByLastNameAscFirstNameAscStudentNumberAsc(
			establishmentId).stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Student> findActiveByEstablishmentIdAndIds(UUID establishmentId, List<UUID> studentIds) {
		if (studentIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository.findAllByEstablishmentIdAndIdInAndEnabledTrue(establishmentId, studentIds)
			.stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Student> findByIdAndEstablishmentId(UUID studentId, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(studentId, establishmentId).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Student> findIncludingArchivedByIdAndEstablishmentId(UUID studentId, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentId(studentId, establishmentId).map(this::toDomain);
	}

	@Override
	@Transactional
	public Student updateProfile(UUID studentId, UUID establishmentId, String firstName, String lastName) {
		StudentEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(studentId, establishmentId)
			.orElseThrow();
		entity.updateProfile(firstName, lastName);
		return toDomain(entity);
	}

	@Override
	@Transactional
	public Student archive(UUID studentId, UUID establishmentId) {
		StudentEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(studentId, establishmentId)
			.orElseThrow();
		entity.archive();
		return toDomain(entity);
	}

	private Student toDomain(StudentEntity entity) {
		return new Student(entity.getId(), entity.getEstablishmentId(), entity.getStudentNumber(),
			entity.getFirstName(), entity.getLastName(), entity.isEnabled(), entity.getCreatedAt(), entity.getUpdatedAt());
	}

	private String nextStudentNumber(UUID establishmentId) {
		String studentNumber;
		do {
			studentNumber = String.format(Locale.ROOT, "STU-%08d", jpaRepository.nextStudentNumber());
		} while (jpaRepository.existsByEstablishmentIdAndStudentNumber(establishmentId, studentNumber));
		return studentNumber;
	}
}