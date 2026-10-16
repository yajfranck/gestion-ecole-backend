package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TuitionFee(UUID id, UUID establishmentId, UUID academicYearId, UUID feeTypeId,
	String feeTypeCode, String feeTypeName, String name,
	BigDecimal amount, String currency, LocalDate dueDate, UUID schoolClassId, UUID studentId,
	boolean enabled, Instant createdAt) {
}