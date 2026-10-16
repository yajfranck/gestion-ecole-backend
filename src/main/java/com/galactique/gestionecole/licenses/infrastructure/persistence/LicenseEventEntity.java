package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.time.Instant;

import com.galactique.gestionecole.licenses.domain.LicenseEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "license_events")
public class LicenseEventEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long licenseId;

	@Column(nullable = false, length = 32)
	private String action;

	@Column(nullable = false, length = 100)
	private String actor;

	@Column(length = 1000)
	private String details;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected LicenseEventEntity() {
	}

	public LicenseEventEntity(Long licenseId, String action, String actor, String details) {
		this.licenseId = licenseId;
		this.action = action;
		this.actor = actor;
		this.details = details;
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public LicenseEvent toDomain() { return new LicenseEvent(id, licenseId, action, actor, details, createdAt); }
}
