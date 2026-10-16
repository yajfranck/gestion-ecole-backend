package com.galactique.gestionecole.learning.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.learning.domain.LearningContent;
import com.galactique.gestionecole.learning.domain.LearningContentDraft;
import com.galactique.gestionecole.learning.domain.LearningContentPage;
import com.galactique.gestionecole.learning.domain.LearningContentRepository;
import com.galactique.gestionecole.learning.domain.LearningContentStatus;
import com.galactique.gestionecole.learning.domain.LearningContentType;

@Repository
public class JpaLearningContentRepositoryAdapter implements LearningContentRepository {
	private final LearningContentJpaRepository jpaRepository;

	public JpaLearningContentRepositoryAdapter(LearningContentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public LearningContent save(LearningContentDraft draft) {
		return jpaRepository.saveAndFlush(new LearningContentEntity(draft.establishmentId(), draft.academicYearId(),
			draft.schoolClassId(), draft.subjectId(), draft.type(), draft.title(), draft.description(),
			draft.body(), draft.dueDate(), draft.username())).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public LearningContentPage findPage(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		LearningContentType type, boolean publishedOnly, int page, int size) {
		LearningContentStatus status = publishedOnly ? LearningContentStatus.PUBLISHED : null;
		var result = jpaRepository.findPage(establishmentId, academicYearId, schoolClassId,
			type, status, LearningContentStatus.ARCHIVED,
			PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
		return new LearningContentPage(result.getContent().stream().map(LearningContentEntity::toDomain).toList(),
			result.getTotalElements(), page, size);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LearningContent> find(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID contentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassId(
			contentId, establishmentId, academicYearId, schoolClassId).map(LearningContentEntity::toDomain);
	}

	@Override
	@Transactional
	public LearningContent update(LearningContent current, String title, String description,
		String body, UUID subjectId, LocalDate dueDate, String username) {
		LearningContentEntity entity = jpaRepository.findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassId(
			current.id(), current.establishmentId(), current.academicYearId(), current.schoolClassId()).orElseThrow();
		entity.update(title, description, body, subjectId, dueDate, username);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public LearningContent publish(LearningContent current, String username) {
		LearningContentEntity entity = findEntity(current);
		entity.publish(username);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public LearningContent archive(LearningContent current, String username) {
		LearningContentEntity entity = findEntity(current);
		entity.archive(username);
		return entity.toDomain();
	}

	private LearningContentEntity findEntity(LearningContent content) {
		return jpaRepository.findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassId(
			content.id(), content.establishmentId(), content.academicYearId(), content.schoolClassId()).orElseThrow();
	}
}
