package com.galactique.gestionecole.learning.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LearningAttachmentRepository {
	LearningAttachment save(UUID contentId, String storageKey, String fileName, String mediaType, long size,
		String uploadedBy);

	List<LearningAttachment> findAllByContentId(UUID contentId);

	Optional<LearningAttachment> findByIdAndContentId(UUID attachmentId, UUID contentId);

	Set<String> findAllStorageKeys();

	void delete(LearningAttachment attachment);
}
