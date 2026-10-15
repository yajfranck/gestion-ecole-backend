package com.galactique.gestionecole.security.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "security_permissions")
public class PermissionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String code;

	@Column(nullable = false, length = 255)
	private String description;

	protected PermissionEntity() {
	}

	PermissionEntity(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}
}