package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseOffer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "license_offers")
public class LicenseOfferEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 40)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "license_offer_features", joinColumns = @JoinColumn(name = "offer_id"))
	@Column(name = "feature_code", nullable = false, length = 64)
	@Enumerated(EnumType.STRING)
	private Set<LicenseFeature> features = new LinkedHashSet<>();

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected LicenseOfferEntity() {
	}

	public LicenseOfferEntity(String code, String name, Set<LicenseFeature> features) {
		this.code = code;
		this.name = name;
		this.features = new LinkedHashSet<>(features);
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public UUID getId() { return id; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public boolean isEnabled() { return enabled; }
	public Set<LicenseFeature> getFeatures() { return Set.copyOf(features); }
	public Instant getCreatedAt() { return createdAt; }

	public LicenseOffer toDomain() {
		return new LicenseOffer(id, code, name, features, enabled, createdAt);
	}
}
