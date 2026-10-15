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

import com.galactique.gestionecole.schoolclasses.domain.GradeLevel;

@Entity
@Table(name = "grade_levels", uniqueConstraints = {
	@UniqueConstraint(name = "uq_grade_levels_establishment_code", columnNames = {"establishment_id", "code"}),
	@UniqueConstraint(name = "uq_grade_levels_establishment_name", columnNames = {"establishment_id", "name"}),
	@UniqueConstraint(name = "uq_grade_levels_id_establishment", columnNames = {"id", "establishment_id"})
})
public class GradeLevelEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected GradeLevelEntity() {
	}

	public GradeLevelEntity(UUID establishmentId, String code, String name, int displayOrder) {
		this.establishmentId = establishmentId;
		this.code = code;
		this.name = name;
		this.displayOrder = displayOrder;
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
	public String getCode() { return code; }
	public String getName() { return name; }
	public int getDisplayOrder() { return displayOrder; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void update(String code, String name, int displayOrder) {
		this.code = code;
		this.name = name;
		this.displayOrder = displayOrder;
	}

	public void archive() {
		this.enabled = false;
	}

	public GradeLevel toDomain() {
		return new GradeLevel(id, establishmentId, code, name, displayOrder, enabled, createdAt, updatedAt);
	}
}