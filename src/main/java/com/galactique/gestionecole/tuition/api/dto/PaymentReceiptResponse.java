package com.galactique.gestionecole.tuition.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.tuition.application.TuitionApplicationService.Receipt;

public record PaymentReceiptResponse(String receiptNumber, UUID paymentId, UUID establishmentId,
	UUID studentId, String studentNumber, String studentFirstName, String studentLastName,
	String feeName, BigDecimal amountPaid, String currency, String paymentMethodCode,
	String paymentMethodName, String reference, Instant receivedAt, String recordedBy, BigDecimal chargeAmount,
	BigDecimal balanceAfterPayment, LocalDate dueDate) {
	public static PaymentReceiptResponse from(Receipt receipt) {
		var payment = receipt.payment();
		var charge = receipt.charge();
		return new PaymentReceiptResponse(payment.receiptNumber(), payment.id(), payment.establishmentId(),
			payment.studentId(), charge.studentNumber(), charge.studentFirstName(), charge.studentLastName(),
			charge.name(), payment.amount(), payment.currency(), payment.paymentMethodCode(),
			payment.paymentMethodName(), payment.reference(),
			payment.receivedAt(), payment.recordedBy(), charge.amount(), receipt.balanceAfterPayment(), charge.dueDate());
	}
}