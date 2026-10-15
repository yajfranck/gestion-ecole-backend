package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.galactique.gestionecole.tuition.domain.TuitionPaymentMethod;

@Entity
@Table(name = "school_payment_methods")
public class TuitionPaymentMethodEntity {
	@Id
	private UUID id;
	@Column(nullable = false)
	private UUID establishmentId;
	@Column(nullable = false, length = 40)
	private String code;
	@Column(nullable = false, length = 100)
	private String name;
	@Column(nullable = false)
	private boolean enabled = true;

	protected TuitionPaymentMethodEntity() { }

	public TuitionPaymentMethodEntity(TuitionPaymentMethod method) {
		id = method.id();
		establishmentId = method.establishmentId();
		code = method.code();
		name = method.name();
		enabled = method.enabled();
	}

	public String getCode() { return code; }

	public TuitionPaymentMethod toDomain() {
		return new TuitionPaymentMethod(id, establishmentId, code, name, enabled);
	}

	public void updateName(String name) { this.name = name; }
	public void disable() { enabled = false; }
	public void enable() { enabled = true; }
}