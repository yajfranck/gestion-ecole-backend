package com.galactique.gestionecole.employees.infrastructure.persistence;

import com.galactique.gestionecole.positions.infrastructure.persistence.PositionEntity;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
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
@Table(name = "employees")
public class EmployeeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 40)
	private String employeeNumber;

	@Column(nullable = false, length = 100)
	private String firstName;

	@Column(nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_id", nullable = false)
	private PositionEntity position;

	@Column(nullable = false)
	private boolean enabled = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "employee_roles", joinColumns = @JoinColumn(name = "employee_id"))
	@Column(name = "role_code", nullable = false, length = 64)
	private Set<String> directRoleCodes = new LinkedHashSet<>();

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(unique = true)
	private UUID userAccountId;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected EmployeeEntity() {
	}

	public EmployeeEntity(String employeeNumber, String firstName, String lastName, String email,
		PositionEntity position, Set<String> directRoleCodes, UUID establishmentId) {
		this.employeeNumber = employeeNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.position = position;
		this.directRoleCodes = new LinkedHashSet<>(directRoleCodes);
		this.establishmentId = establishmentId;
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
	public String getEmployeeNumber() { return employeeNumber; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public String getEmail() { return email; }
	public PositionEntity getPosition() { return position; }
	public boolean isEnabled() { return enabled; }
	public Set<String> getDirectRoleCodes() { return Set.copyOf(directRoleCodes); }
	public UUID getEstablishmentId() { return establishmentId; }
	public UUID getUserAccountId() { return userAccountId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void attachUserAccount(UUID userAccountId) {
		this.userAccountId = userAccountId;
	}

	public void updateProfile(String employeeNumber, String firstName, String lastName, String email,
		PositionEntity position) {
		this.employeeNumber = employeeNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.position = position;
		this.establishmentId = position.getEstablishmentId();
	}

	public void updateRoles(Set<String> directRoleCodes) {
		this.directRoleCodes = new LinkedHashSet<>(directRoleCodes);
	}

	public void disable() {
		this.enabled = false;
	}
}