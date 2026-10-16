package com.galactique.gestionecole.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.learning.domain.LearningAttachment;
import com.galactique.gestionecole.learning.domain.LearningAttachmentRepository;

@Repository
public class JpaLearningAttachmentRepositoryAdapter implements LearningAttachmentRepository {
	private final LearningAttachmentJpaRepository jpaRepository;

	public JpaLearningAttachmentRepositoryAdapter(LearningAttachmentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public LearningAttachment save(UUID contentId, String storageKey, String fileName,
		String mediaType, long size, String uploadedBy) {
		return jpaRepository.saveAndFlush(new LearningAttachmentEntity(
			contentId, storageKey, fileName, mediaType, size, uploadedBy)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<LearningAttachment> findAllByContentId(UUID contentId) {
		return jpaRepository.findAllByContentIdOrderByCreatedAtAsc(contentId).stream()
			.map(LearningAttachmentEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LearningAttachment> findByIdAndContentId(UUID attachmentId, UUID contentId) {
		return jpaRepository.findByIdAndContentId(attachmentId, contentId).map(LearningAttachmentEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Set<String> findAllStorageKeys() {
		return jpaRepository.findAllStorageKeys();
	}

	@Override
	@Transactional
	public void delete(LearningAttachment attachment) {
		jpaRepository.deleteById(attachment.id());
	}
}
