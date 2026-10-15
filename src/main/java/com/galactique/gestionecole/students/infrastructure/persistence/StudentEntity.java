package com.galactique.gestionecole.students.infrastructure.persistence;

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

@Entity
@Table(name = "students", uniqueConstraints =
	@UniqueConstraint(name = "uq_students_establishment_number", columnNames = {"establishment_id", "student_number"}))
public class StudentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false, length = 40)
	private String studentNumber;

	@Column(nullable = false, length = 100)
	private String firstName;

	@Column(nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected StudentEntity() {
	}

	public StudentEntity(UUID establishmentId, String studentNumber, String firstName, String lastName) {
		this.establishmentId = establishmentId;
		this.studentNumber = studentNumber;
		this.firstName = firstName;
		this.lastName = lastName;
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
	public String getStudentNumber() { return studentNumber; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void updateProfile(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public void archive() {
		this.enabled = false;
	}
}