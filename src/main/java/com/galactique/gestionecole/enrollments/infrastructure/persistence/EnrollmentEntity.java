package com.galactique.gestionecole.enrollments.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.galactique.gestionecole.enrollments.domain.Enrollment;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(
	name = "uq_enrollments_student_year", columnNames = {"student_id", "academic_year_id"}))
public class EnrollmentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID establishmentId;

	@Column(nullable = false)
	private UUID studentId;

	@Column(nullable = false)
	private UUID academicYearId;

	@Column(nullable = false)
	private UUID schoolClassId;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected EnrollmentEntity() {
	}

	public EnrollmentEntity(UUID establishmentId, UUID studentId, UUID academicYearId, UUID schoolClassId) {
		this.establishmentId = establishmentId;
		this.studentId = studentId;
		this.academicYearId = academicYearId;
		this.schoolClassId = schoolClassId;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Enrollment toDomain() {
		return new Enrollment(id, establishmentId, studentId, academicYearId, schoolClassId, createdAt);
	}
}