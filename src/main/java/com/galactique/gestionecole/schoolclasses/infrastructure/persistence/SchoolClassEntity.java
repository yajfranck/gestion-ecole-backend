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

import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;

@Entity
@Table(name = "school_classes", uniqueConstraints = {
	@UniqueConstraint(name = "uq_school_classes_year_code", columnNames = {"academic_year_id", "code"}),
	@UniqueConstraint(name = "uq_school_classes_year_level_name",
		columnNames = {"academic_year_id", "grade_level_id", "name"}),
	@UniqueConstraint(name = "uq_school_classes_id_establishment", columnNames = {"id", "establishment_id"})
})
public class SchoolClassEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false)
	private UUID academicYearId;

	@Column(nullable = false)
	private UUID gradeLevelId;

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected SchoolClassEntity() {
	}

	public SchoolClassEntity(UUID establishmentId, UUID academicYearId, UUID gradeLevelId,
		String code, String name) {
		this.establishmentId = establishmentId;
		this.academicYearId = academicYearId;
		this.gradeLevelId = gradeLevelId;
		this.code = code;
		this.name = name;
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
	public UUID getAcademicYearId() { return academicYearId; }
	public UUID getGradeLevelId() { return gradeLevelId; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void update(UUID gradeLevelId, String code, String name) {
		this.gradeLevelId = gradeLevelId;
		this.code = code;
		this.name = name;
	}

	public void archive() {
		this.enabled = false;
	}

	public SchoolClass toDomain() {
		return new SchoolClass(id, establishmentId, academicYearId, gradeLevelId, code, name,
			enabled, createdAt, updatedAt);
	}
}