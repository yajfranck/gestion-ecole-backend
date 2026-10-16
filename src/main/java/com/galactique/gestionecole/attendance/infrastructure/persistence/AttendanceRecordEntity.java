package com.galactique.gestionecole.attendance.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;
import com.galactique.gestionecole.attendance.domain.AttendanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecordEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID enrollmentId;

	@Column(nullable = false)
	private LocalDate attendanceDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private AttendanceStatus status;

	@Column(nullable = false)
	private boolean absenceJustified;

	@Column(length = 500)
	private String comment;

	@Column(length = 500)
	private String parentComment;

	@Column(nullable = false, updatable = false, length = 100)
	private String createdBy;

	@Column(nullable = false, length = 100)
	private String updatedBy;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected AttendanceRecordEntity() {
	}

	public AttendanceRecordEntity(UUID enrollmentId, LocalDate attendanceDate, AttendanceStatus status,
		boolean absenceJustified, String comment, String parentComment, String username) {
		this.enrollmentId = enrollmentId;
		this.attendanceDate = attendanceDate;
		this.status = status;
		this.absenceJustified = absenceJustified;
		this.comment = comment;
		this.parentComment = parentComment;
		this.createdBy = username;
		this.updatedBy = username;
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
	public UUID getEnrollmentId() { return enrollmentId; }
	public LocalDate getAttendanceDate() { return attendanceDate; }

	public void update(AttendanceStatus status, boolean absenceJustified,
		String comment, String parentComment, String username) {
		this.status = status;
		this.absenceJustified = absenceJustified;
		this.comment = comment;
		this.parentComment = parentComment;
		this.updatedBy = username;
	}

	public AttendanceRecord toDomain() {
		return new AttendanceRecord(id, enrollmentId, attendanceDate, status, absenceJustified,
			comment, parentComment, createdBy, updatedBy, createdAt, updatedAt);
	}
}