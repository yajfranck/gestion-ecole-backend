package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TuitionPayment(UUID id, UUID chargeId, UUID establishmentId, UUID studentId,
	String receiptNumber, BigDecimal amount, String currency, String paymentMethodCode, String paymentMethodName,
	String reference, Instant receivedAt, String recordedBy) {
}