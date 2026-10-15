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

import com.galactique.gestionecole.schoolclasses.domain.AcademicPeriod;

@Entity
@Table(name = "academic_periods", uniqueConstraints =
	@UniqueConstraint(name = "uq_academic_periods_year_name", columnNames = {"academic_year_id", "name"}))
public class AcademicPeriodEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID academicYearId;

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

	protected AcademicPeriodEntity() {
	}

	public AcademicPeriodEntity(UUID academicYearId, String name, LocalDate startsOn, LocalDate endsOn) {
		this.academicYearId = academicYearId;
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
	public UUID getAcademicYearId() { return academicYearId; }
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

	public AcademicPeriod toDomain() {
		return new AcademicPeriod(id, academicYearId, name, startsOn, endsOn, enabled, createdAt, updatedAt);
	}
}