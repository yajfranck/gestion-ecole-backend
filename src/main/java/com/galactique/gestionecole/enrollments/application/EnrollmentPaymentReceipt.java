package com.galactique.gestionecole.enrollments.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnrollmentPaymentReceipt(UUID id, UUID feeId, UUID chargeId, String receiptNumber,
	BigDecimal amount, String currency, String paymentMethodCode, String paymentMethodName, String reference,
	Instant receivedAt, String recordedBy, BigDecimal totalPaid, BigDecimal balance) { }
