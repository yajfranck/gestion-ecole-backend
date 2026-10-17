package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

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
import com.galactique.gestionecole.schoolclasses.domain.Classroom;

@Entity
@Table(name = "classrooms", uniqueConstraints = @UniqueConstraint(name = "uq_classrooms_class_code",
	columnNames = {"school_class_id", "code"}))
public class ClassroomEntity {
	@Id @GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(nullable = false) private UUID establishmentId;
	@Column(nullable = false) private UUID academicYearId;
	@Column(nullable = false) private UUID schoolClassId;
	@Column(nullable = false, length = 40) private String code;
	@Column(nullable = false, length = 100) private String name;
	@Column(nullable = false) private boolean enabled = true;
	@Column(nullable = false, updatable = false) private Instant createdAt;
	@Column(nullable = false) private Instant updatedAt;

	protected ClassroomEntity() { }

	public ClassroomEntity(UUID establishmentId, UUID academicYearId, UUID schoolClassId, String code, String name) {
		this.establishmentId = establishmentId;
		this.academicYearId = academicYearId;
		this.schoolClassId = schoolClassId;
		update(code, name);
	}

	@PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
	@PreUpdate void onUpdate() { updatedAt = Instant.now(); }

	public void update(String code, String name) { this.code = code; this.name = name; }
	public void archive() { enabled = false; }
	public Classroom toDomain() {
		return new Classroom(id, establishmentId, academicYearId, schoolClassId, code, name,
			enabled, createdAt, updatedAt);
	}
}
