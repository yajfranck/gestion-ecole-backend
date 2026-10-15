package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
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

import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;

@Entity
@Table(name = "academic_years", uniqueConstraints =
	@UniqueConstraint(name = "uq_academic_years_establishment_name", columnNames = {"establishment_id", "name"}))
public class AcademicYearEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private LocalDate startsOn;

	@Column(nullable = false)
	private LocalDate endsOn;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected AcademicYearEntity() {
	}

	public AcademicYearEntity(UUID establishmentId, String name, LocalDate startsOn, LocalDate endsOn) {
		this.establishmentId = establishmentId;
		this.name = name;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
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
	public String getName() { return name; }
	public LocalDate getStartsOn() { return startsOn; }
	public LocalDate getEndsOn() { return endsOn; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void update(String name, LocalDate startsOn, LocalDate endsOn) {
		this.name = name;
		this.startsOn = startsOn;
		this.endsOn = endsOn;
	}

	public void disable() {
		this.enabled = false;
	}

	public AcademicYear toDomain() {
		return new AcademicYear(id, establishmentId, name, startsOn, endsOn, enabled, createdAt, updatedAt);
	}
}