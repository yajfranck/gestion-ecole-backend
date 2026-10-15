package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guardian_student_links")
public class GuardianStudentLinkEntity {
	@Id
	private UUID id;
	@Column(nullable = false)
	private UUID guardianId;
	@Column(nullable = false)
	private UUID studentId;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected GuardianStudentLinkEntity() {
	}

	public GuardianStudentLinkEntity(UUID guardianId, UUID studentId) {
		this.id = UUID.randomUUID();
		this.guardianId = guardianId;
		this.studentId = studentId;
		this.createdAt = Instant.now();
	}
}