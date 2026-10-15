package com.galactique.gestionecole.security.infrastructure.persistence;

import java.time.Instant;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_user_accounts")
public class UserAccountEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false)
	private boolean locked;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Column
	private Instant lastLoginAt;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "security_user_roles",
		joinColumns = @JoinColumn(name = "user_account_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<RoleEntity> roles = new LinkedHashSet<>();

	protected UserAccountEntity() {
	}

	UserAccountEntity(String username, String passwordHash, Set<RoleEntity> roles) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.enabled = true;
		this.locked = false;
		this.roles = new LinkedHashSet<>(roles);
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

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isLocked() {
		return locked;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public Set<RoleEntity> getRoles() {
		return Set.copyOf(roles);
	}

	void updateEmployeeAccount(String username, Set<RoleEntity> roles) {
		this.username = username;
		this.roles = new LinkedHashSet<>(roles);
	}

	void disable() {
		this.enabled = false;
	}

	void changePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}