package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.ClassSubjectRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassSubject;

@Repository
public class JpaClassSubjectRepositoryAdapter implements ClassSubjectRepository {
	private final SchoolClassSubjectJpaRepository jpaRepository;

	public JpaClassSubjectRepositoryAdapter(SchoolClassSubjectJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SchoolClassSubject> findByEstablishmentIdAndClassIdAndSubjectId(
		UUID establishmentId, UUID schoolClassId, UUID subjectId) {
		return jpaRepository.findByEstablishmentIdAndSchoolClassIdAndSubjectId(
			establishmentId, schoolClassId, subjectId).map(SchoolClassSubjectEntity::toDomain);
	}

	@Override
	@Transactional
	public SchoolClassSubject save(UUID establishmentId, UUID schoolClassId, UUID subjectId) {
		return jpaRepository.save(new SchoolClassSubjectEntity(establishmentId, schoolClassId, subjectId)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SchoolClassSubject> findAllByEstablishmentIdAndClassId(UUID establishmentId, UUID schoolClassId) {
		return jpaRepository.findAllByEstablishmentIdAndSchoolClassIdAndEnabledTrueOrderByCreatedAtAsc(
			establishmentId, schoolClassId).stream().map(SchoolClassSubjectEntity::toDomain).toList();
	}

	@Override
	@Transactional
	public SchoolClassSubject enable(UUID assignmentId, UUID establishmentId) {
		SchoolClassSubjectEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(assignmentId, establishmentId)
			.or(() -> jpaRepository.findById(assignmentId).filter(existing -> existing.getEstablishmentId().equals(establishmentId)))
			.orElseThrow();
		entity.enable();
		return entity.toDomain();
	}

	@Override
	@Transactional
	public SchoolClassSubject disable(UUID assignmentId, UUID establishmentId) {
		SchoolClassSubjectEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(assignmentId, establishmentId)
			.orElseThrow();
		entity.disable();
		return entity.toDomain();
	}
}