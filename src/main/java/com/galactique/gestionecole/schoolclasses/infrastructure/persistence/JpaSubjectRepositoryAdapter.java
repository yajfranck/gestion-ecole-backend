package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.Subject;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;

@Repository
public class JpaSubjectRepositoryAdapter implements SubjectRepository {
	private final SubjectJpaRepository jpaRepository;
	private final SchoolClassSubjectJpaRepository classSubjectJpaRepository;

	public JpaSubjectRepositoryAdapter(SubjectJpaRepository jpaRepository,
		SchoolClassSubjectJpaRepository classSubjectJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.classSubjectJpaRepository = classSubjectJpaRepository;
	}

	@Override
	@Transactional
	public Subject save(UUID establishmentId, String code, String name) {
		return jpaRepository.save(new SubjectEntity(establishmentId, code, name)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndCode(UUID establishmentId, String code) {
		return jpaRepository.existsByEstablishmentIdAndCodeIgnoreCase(establishmentId, code);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndCodeAndIdNot(UUID establishmentId, String code, UUID subjectId) {
		return jpaRepository.existsByEstablishmentIdAndCodeIgnoreCaseAndIdNot(establishmentId, code, subjectId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasActiveClassAssignments(UUID establishmentId, UUID subjectId) {
		return classSubjectJpaRepository.existsByEstablishmentIdAndSubjectIdAndEnabledTrue(establishmentId, subjectId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Subject> findAllByEstablishmentId(UUID establishmentId) {
		return jpaRepository.findAllByEstablishmentIdAndEnabledTrueOrderByNameAsc(establishmentId).stream()
			.map(SubjectEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Subject> findByIdAndEstablishmentId(UUID subjectId, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(subjectId, establishmentId)
			.map(SubjectEntity::toDomain);
	}

	@Override
	@Transactional
	public Subject update(UUID subjectId, UUID establishmentId, String code, String name) {
		SubjectEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(subjectId, establishmentId)
			.orElseThrow();
		entity.update(code, name);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public Subject archive(UUID subjectId, UUID establishmentId) {
		SubjectEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(subjectId, establishmentId)
			.orElseThrow();
		entity.archive();
		return entity.toDomain();
	}
}