package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public record TuitionFeeDraft(UUID establishmentId, UUID academicYearId, UUID feeTypeId,
	String feeTypeCode, String feeTypeName, String name,
	BigDecimal amount, String currency, LocalDate dueDate, List<UUID> schoolClassIds, UUID studentId) {
	public TuitionFeeDraft {
		schoolClassIds = List.copyOf(schoolClassIds);
	}
}
