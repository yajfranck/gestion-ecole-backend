package com.galactique.gestionecole.tuition.infrastructure.persistence;

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

import com.galactique.gestionecole.tuition.domain.TuitionFeeType;
import com.galactique.gestionecole.tuition.domain.TuitionFeeTypeDraft;

@Entity
@Table(name = "tuition_fee_types", uniqueConstraints = {
	@UniqueConstraint(name = "uq_tuition_fee_type_establishment_code", columnNames = {"establishment_id", "code"}),
	@UniqueConstraint(name = "uq_tuition_fee_type_establishment_name", columnNames = {"establishment_id", "name"})
})
public class TuitionFeeTypeEntity {
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
	private boolean enabled = true;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	@Column(nullable = false)
	private Instant updatedAt;

	protected TuitionFeeTypeEntity() {
	}

	public TuitionFeeTypeEntity(TuitionFeeTypeDraft draft) {
		establishmentId = draft.establishmentId();
		code = draft.code();
		name = draft.name();
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
	public String getCode() { return code; }
	public String getName() { return name; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void updateName(String name) { this.name = name; }
	public void disable() { enabled = false; }
	public TuitionFeeType toDomain() {
		return new TuitionFeeType(id, establishmentId, code, name, enabled, createdAt, updatedAt);
	}
}