package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.guardians.domain.Guardian;
import com.galactique.gestionecole.guardians.domain.GuardianRepository;
import com.galactique.gestionecole.students.domain.Student;

@Repository
public class JpaGuardianRepositoryAdapter implements GuardianRepository {
	private final GuardianJpaRepository guardianJpaRepository;
	private final GuardianStudentLinkJpaRepository linkJpaRepository;

	public JpaGuardianRepositoryAdapter(GuardianJpaRepository guardianJpaRepository,
		GuardianStudentLinkJpaRepository linkJpaRepository) {
		this.guardianJpaRepository = guardianJpaRepository;
		this.linkJpaRepository = linkJpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Guardian> findByEmail(String email) {
		return guardianJpaRepository.findByEmailIgnoreCase(email).map(guardian -> guardian.toDomain());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Guardian> findByUserAccountId(UUID userAccountId) {
		return guardianJpaRepository.findByUserAccountId(userAccountId).map(GuardianEntity::toDomain);
	}

	@Override
	@Transactional
	public Optional<Guardian> findByIdForUpdate(UUID guardianId) {
		return guardianJpaRepository.findByIdForUpdate(guardianId).map(guardian -> guardian.toDomain());
	}

	@Override
	@Transactional
	public Guardian save(String firstName, String lastName, String email) {
		return guardianJpaRepository.save(new GuardianEntity(firstName, lastName, email)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsLink(UUID guardianId, UUID studentId) {
		return linkJpaRepository.existsByGuardianIdAndStudentId(guardianId, studentId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Student> findLinkedChild(UUID guardianId, UUID studentId) {
		return linkJpaRepository.findLinkedActiveChild(guardianId, studentId).map(student ->
			new Student(student.getId(), student.getEstablishmentId(), student.getStudentNumber(),
				student.getFirstName(), student.getLastName(), student.isEnabled(),
				student.getCreatedAt(), student.getUpdatedAt()));
	}

	@Override
	@Transactional
	public void link(UUID guardianId, UUID studentId) {
		linkJpaRepository.save(new GuardianStudentLinkEntity(guardianId, studentId));
	}

	@Override
	@Transactional
	public void unlink(UUID guardianId, UUID studentId) {
		linkJpaRepository.deleteByGuardianIdAndStudentId(guardianId, studentId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Guardian> findAllByStudentId(UUID studentId) {
		return linkJpaRepository.findGuardiansByStudentId(studentId).stream()
			.map(guardian -> guardian.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Student> findChildren(UUID guardianId) {
		return linkJpaRepository.findChildren(guardianId).stream()
			.map(student -> new Student(student.getId(), student.getEstablishmentId(), student.getStudentNumber(),
				student.getFirstName(), student.getLastName(), student.isEnabled(), student.getCreatedAt(), student.getUpdatedAt()))
			.toList();
	}

	@Override
	@Transactional
	public Guardian attachUserAccount(UUID guardianId, UUID userAccountId) {
		GuardianEntity guardian = guardianJpaRepository.findByIdForUpdate(guardianId).orElseThrow();
		guardian.attachUserAccount(userAccountId);
		return guardian.toDomain();
	}
}
