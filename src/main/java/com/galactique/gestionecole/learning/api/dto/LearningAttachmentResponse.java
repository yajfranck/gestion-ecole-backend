package com.galactique.gestionecole.learning.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.learning.domain.LearningAttachment;

public record LearningAttachmentResponse(UUID id, UUID contentId, String fileName,
	String mediaType, long size, Instant createdAt) {
	public static LearningAttachmentResponse from(LearningAttachment attachment) {
		return new LearningAttachmentResponse(attachment.id(), attachment.contentId(), attachment.fileName(),
			attachment.mediaType(), attachment.size(), attachment.createdAt());
	}
}
