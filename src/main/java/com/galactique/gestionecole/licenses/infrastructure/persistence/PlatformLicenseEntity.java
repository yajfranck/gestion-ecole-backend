package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.licenses.domain.PlatformLicense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_licenses")
public class PlatformLicenseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private UUID establishmentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "offer_id", nullable = false)
	private LicenseOfferEntity offer;

	@Column(nullable = false)
	private Instant validFrom;

	@Column
	private Instant validUntil;

	@Column
	private Instant suspendedAt;

	@Column(nullable = false, length = 100)
	private String createdBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected PlatformLicenseEntity() {
	}

	public PlatformLicenseEntity(UUID establishmentId, LicenseOfferEntity offer, Instant validFrom,
		Instant validUntil, String createdBy) {
		this.establishmentId = establishmentId;
		this.offer = offer;
		this.validFrom = validFrom;
		this.validUntil = validUntil;
		this.createdBy = createdBy;
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

	public Long getId() { return id; }
	public UUID getEstablishmentId() { return establishmentId; }
	public LicenseOfferEntity getOffer() { return offer; }
	public Instant getValidFrom() { return validFrom; }
	public Instant getValidUntil() { return validUntil; }
	public Instant getSuspendedAt() { return suspendedAt; }
	public String getCreatedBy() { return createdBy; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void setValidUntil(Instant validUntil) {
		this.validUntil = validUntil;
	}

	public void suspend() {
		this.suspendedAt = Instant.now();
	}

	public void reactivate() {
		this.suspendedAt = null;
	}

	public PlatformLicense toDomain() {
		return new PlatformLicense(id, establishmentId, offer.getId(), offer.getCode(), offer.getName(),
			validFrom, validUntil, suspendedAt, createdBy, createdAt);
	}
}
