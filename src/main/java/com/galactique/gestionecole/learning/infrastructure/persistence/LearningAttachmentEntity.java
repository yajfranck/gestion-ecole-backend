package com.galactique.gestionecole.learning.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.learning.domain.LearningAttachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_attachments")
public class LearningAttachmentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID contentId;

	@Column(nullable = false, unique = true, length = 80)
	private String storageKey;

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(nullable = false, length = 120)
	private String mediaType;

	@Column(nullable = false)
	private long size;

	@Column(nullable = false, length = 100)
	private String uploadedBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected LearningAttachmentEntity() {
	}

	public LearningAttachmentEntity(UUID contentId, String storageKey, String fileName,
		String mediaType, long size, String uploadedBy) {
		this.contentId = contentId;
		this.storageKey = storageKey;
		this.fileName = fileName;
		this.mediaType = mediaType;
		this.size = size;
		this.uploadedBy = uploadedBy;
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public UUID getId() { return id; }
	public UUID getContentId() { return contentId; }
	public String getStorageKey() { return storageKey; }
	public String getFileName() { return fileName; }
	public String getMediaType() { return mediaType; }
	public long getSize() { return size; }
	public String getUploadedBy() { return uploadedBy; }

	public LearningAttachment toDomain() {
		return new LearningAttachment(id, contentId, storageKey, fileName, mediaType, size, uploadedBy, createdAt);
	}
}
