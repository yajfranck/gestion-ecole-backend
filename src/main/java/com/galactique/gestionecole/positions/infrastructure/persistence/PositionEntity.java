package com.galactique.gestionecole.positions.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "positions", uniqueConstraints =
	@UniqueConstraint(name = "uq_positions_establishment_name", columnNames = {"establishment_id", "name"}))
public class PositionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false)
	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "position_roles", joinColumns = @JoinColumn(name = "position_id"))
	@Column(name = "role_code", nullable = false, length = 64)
	private Set<String> roleCodes = new LinkedHashSet<>();

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected PositionEntity() {
	}

	public PositionEntity(UUID establishmentId, String name, Set<String> roleCodes) {
		this.establishmentId = establishmentId;
		this.name = name;
		this.roleCodes = new LinkedHashSet<>(roleCodes);
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
	public boolean isEnabled() { return enabled; }
	public Set<String> getRoleCodes() { return Set.copyOf(roleCodes); }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void updateName(String name) {
		this.name = name;
	}

	public void updateRoleCodes(Set<String> roleCodes) {
		this.roleCodes.clear();
		this.roleCodes.addAll(roleCodes);
	}

	public void disable() {
		this.enabled = false;
	}
}