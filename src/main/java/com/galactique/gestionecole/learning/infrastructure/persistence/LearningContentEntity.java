package com.galactique.gestionecole.learning.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.learning.domain.LearningContent;
import com.galactique.gestionecole.learning.domain.LearningContentStatus;
import com.galactique.gestionecole.learning.domain.LearningContentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_contents")
public class LearningContentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false)
	private UUID academicYearId;

	@Column(nullable = false)
	private UUID schoolClassId;

	@Column(nullable = false)
	private UUID subjectId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private LearningContentType type;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(length = 2000)
	private String description;

	@Column(columnDefinition = "TEXT")
	private String body;

	@Column
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private LearningContentStatus status = LearningContentStatus.DRAFT;

	@Column(nullable = false, length = 100)
	private String createdBy;

	@Column(nullable = false, length = 100)
	private String updatedBy;

	@Column(length = 100)
	private String publishedBy;

	@Column
	private Instant publishedAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected LearningContentEntity() {
	}

	public LearningContentEntity(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID subjectId, LearningContentType type, String title, String description, String body,
		LocalDate dueDate, String username) {
		this.establishmentId = establishmentId;
		this.academicYearId = academicYearId;
		this.schoolClassId = schoolClassId;
		this.subjectId = subjectId;
		this.type = type;
		this.title = title;
		this.description = description;
		this.body = body;
		this.dueDate = dueDate;
		this.createdBy = username;
		this.updatedBy = username;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() { updatedAt = Instant.now(); }

	public UUID getId() { return id; }
	public UUID getEstablishmentId() { return establishmentId; }
	public UUID getAcademicYearId() { return academicYearId; }
	public UUID getSchoolClassId() { return schoolClassId; }
	public UUID getSubjectId() { return subjectId; }
	public LearningContentStatus getStatus() { return status; }

	public void update(String title, String description, String body, UUID subjectId,
		LocalDate dueDate, String username) {
		this.title = title;
		this.description = description;
		this.body = body;
		this.subjectId = subjectId;
		this.dueDate = dueDate;
		this.updatedBy = username;
		this.status = LearningContentStatus.DRAFT;
		this.publishedBy = null;
		this.publishedAt = null;
	}

	public void publish(String username) {
		status = LearningContentStatus.PUBLISHED;
		publishedBy = username;
		publishedAt = Instant.now();
		updatedBy = username;
	}

	public void archive(String username) {
		status = LearningContentStatus.ARCHIVED;
		updatedBy = username;
	}

	public LearningContent toDomain() {
		return new LearningContent(id, establishmentId, academicYearId, schoolClassId, subjectId,
			type, title, description, body, dueDate, status, createdBy, updatedBy,
			publishedBy, publishedAt, createdAt, updatedAt);
	}
}
