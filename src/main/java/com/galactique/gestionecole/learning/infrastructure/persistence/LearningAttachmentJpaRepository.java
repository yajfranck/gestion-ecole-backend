package com.galactique.gestionecole.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LearningAttachmentJpaRepository extends JpaRepository<LearningAttachmentEntity, UUID> {
	List<LearningAttachmentEntity> findAllByContentIdOrderByCreatedAtAsc(UUID contentId);

	Optional<LearningAttachmentEntity> findByIdAndContentId(UUID id, UUID contentId);

	@Query("select attachment.storageKey from LearningAttachmentEntity attachment")
	Set<String> findAllStorageKeys();
}
