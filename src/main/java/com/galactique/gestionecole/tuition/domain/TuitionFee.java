package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public record TuitionFee(UUID id, UUID establishmentId, UUID academicYearId, UUID feeTypeId,
	String feeTypeCode, String feeTypeName, String name,
	BigDecimal amount, String currency, LocalDate dueDate, List<UUID> schoolClassIds, UUID studentId,
	boolean enabled, Instant createdAt) {
	public TuitionFee {
		schoolClassIds = List.copyOf(schoolClassIds);
	}

	public UUID schoolClassId() {
		return schoolClassIds.size() == 1 ? schoolClassIds.getFirst() : null;
	}
}
