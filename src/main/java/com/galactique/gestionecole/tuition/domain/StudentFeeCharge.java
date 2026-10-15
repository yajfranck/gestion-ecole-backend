package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentFeeCharge(UUID id, UUID feeId, UUID establishmentId, UUID academicYearId,
	UUID studentId, String studentNumber, String studentFirstName, String studentLastName,
	String name, BigDecimal amount, String currency, LocalDate dueDate, Instant createdAt) {
}