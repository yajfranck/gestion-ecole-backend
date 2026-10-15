package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_roles")
public class RoleEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 255)
	private String description;

	@Column(nullable = false)
	private boolean enabled = true;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "security_role_permissions",
		joinColumns = @JoinColumn(name = "role_id"),
		inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<PermissionEntity> permissions = new LinkedHashSet<>();

	protected RoleEntity() {
	}

	RoleEntity(String code, String name, String description, boolean enabled, Set<PermissionEntity> permissions) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.enabled = enabled;
		this.permissions = new LinkedHashSet<>(permissions);
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Set<PermissionEntity> getPermissions() {
		return Set.copyOf(permissions);
	}

	void updateDetails(String name, String description, Set<PermissionEntity> permissions) {
		this.name = name;
		this.description = description;
		this.permissions = new LinkedHashSet<>(permissions);
	}
}