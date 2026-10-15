package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TuitionFeeDraft(UUID establishmentId, UUID academicYearId, UUID feeTypeId,
	String feeTypeCode, String feeTypeName, String name,
	BigDecimal amount, String currency, LocalDate dueDate, UUID gradeLevelId, UUID studentId) {
}