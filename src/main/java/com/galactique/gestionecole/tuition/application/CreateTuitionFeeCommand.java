package com.galactique.gestionecole.tuition.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTuitionFeeCommand(String name, BigDecimal amount, String currency,
	LocalDate dueDate, UUID feeTypeId, UUID schoolClassId, UUID studentId) {
}