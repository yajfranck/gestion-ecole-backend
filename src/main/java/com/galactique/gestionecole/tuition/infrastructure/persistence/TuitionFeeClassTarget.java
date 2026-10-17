package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TuitionFeeClassTarget {
	@Column(nullable = false)
	private UUID establishmentId;
	@Column(nullable = false)
	private UUID academicYearId;
	@Column(nullable = false)
	private UUID schoolClassId;

	protected TuitionFeeClassTarget() {
	}

	TuitionFeeClassTarget(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		this.establishmentId = establishmentId;
		this.academicYearId = academicYearId;
		this.schoolClassId = schoolClassId;
	}

	UUID getSchoolClassId() {
		return schoolClassId;
	}
}
