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

	@Column(nullable = false)
	private UUID classroomId;

	@Column(nullable = false, updatable = false)
	private boolean advanceAccepted;

	@Column(length = 255, updatable = false)
	private String paymentDecisionBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected EnrollmentEntity() {
	}

	public EnrollmentEntity(UUID establishmentId, UUID studentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
		boolean advanceAccepted, String paymentDecisionBy) {
		this.establishmentId = establishmentId;
		this.studentId = studentId;
		this.academicYearId = academicYearId;
		this.schoolClassId = schoolClassId;
		this.classroomId = classroomId;
		this.advanceAccepted = advanceAccepted;
		this.paymentDecisionBy = paymentDecisionBy;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Enrollment toDomain() {
		return new Enrollment(id, establishmentId, studentId, academicYearId, schoolClassId, classroomId,
			advanceAccepted, paymentDecisionBy, createdAt);
	}

	public void changeClassroom(UUID classroomId) { this.classroomId = classroomId; }
}
