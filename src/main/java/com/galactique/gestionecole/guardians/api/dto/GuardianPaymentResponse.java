package com.galactique.gestionecole.guardians.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.tuition.domain.TuitionPayment;

public record GuardianPaymentResponse(UUID id, String receiptNumber, BigDecimal amount, String currency,
	String paymentMethodName, String reference, Instant receivedAt) {
	public static GuardianPaymentResponse from(TuitionPayment payment) {
		return new GuardianPaymentResponse(payment.id(), payment.receiptNumber(), payment.amount(),
			payment.currency(), payment.paymentMethodName(), payment.reference(), payment.receivedAt());
	}
}
