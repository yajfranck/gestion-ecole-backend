package com.galactique.gestionecole.tuition.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.tuition.domain.TuitionPayment;

public record TuitionPaymentResponse(UUID id, String receiptNumber, BigDecimal amount,
	String currency, String paymentMethodCode, String paymentMethodName, String reference,
	Instant receivedAt, String recordedBy) {
	public static TuitionPaymentResponse from(TuitionPayment payment) {
		return new TuitionPaymentResponse(payment.id(), payment.receiptNumber(), payment.amount(), payment.currency(),
			payment.paymentMethodCode(), payment.paymentMethodName(), payment.reference(),
			payment.receivedAt(), payment.recordedBy());
	}
}