package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.galactique.gestionecole.schoolclasses.domain.SchoolClassSubject;

@Entity
@Table(name = "school_class_subjects", uniqueConstraints = {
	@UniqueConstraint(name = "uq_school_class_subjects_class_subject", columnNames = {"school_class_id", "subject_id"})
})
public class SchoolClassSubjectEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false)
	private UUID schoolClassId;

	@Column(nullable = false)
	private UUID subjectId;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected SchoolClassSubjectEntity() {
	}

	public SchoolClassSubjectEntity(UUID establishmentId, UUID schoolClassId, UUID subjectId) {
		this.establishmentId = establishmentId;
		this.schoolClassId = schoolClassId;
		this.subjectId = subjectId;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UUID getEstablishmentId() { return establishmentId; }
	public UUID getSchoolClassId() { return schoolClassId; }
	public UUID getSubjectId() { return subjectId; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void enable() { this.enabled = true; }
	public void disable() { this.enabled = false; }

	public SchoolClassSubject toDomain() {
		return new SchoolClassSubject(id, establishmentId, schoolClassId, subjectId, enabled, createdAt, updatedAt);
	}
}