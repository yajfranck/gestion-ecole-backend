package com.galactique.gestionecole.learning.domain;

import java.time.Instant;
import java.util.UUID;

public record LearningAttachment(UUID id, UUID contentId, String storageKey, String fileName,
	String mediaType, long size, String uploadedBy, Instant createdAt) {
}
