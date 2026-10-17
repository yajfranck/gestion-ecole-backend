package com.galactique.gestionecole.enrollments.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.galactique.gestionecole.enrollments.application.EnrollmentCreationResult;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentReceipt;

public record EnrollmentCreationResponse(UUID id, UUID establishmentId, UUID studentId, UUID academicYearId,
	UUID schoolClassId, UUID classroomId, boolean advanceAccepted, String paymentDecisionBy, Instant createdAt,
	List<PaymentResponse> payments) {
	public static EnrollmentCreationResponse from(EnrollmentCreationResult result) {
		var enrollment = result.enrollment();
		return new EnrollmentCreationResponse(enrollment.id(), enrollment.establishmentId(), enrollment.studentId(),
			enrollment.academicYearId(), enrollment.schoolClassId(), enrollment.classroomId(), enrollment.advanceAccepted(),
			enrollment.paymentDecisionBy(), enrollment.createdAt(), result.payments().stream().map(PaymentResponse::from).toList());
	}

	public record PaymentResponse(UUID id, UUID feeId, UUID chargeId, String receiptNumber, BigDecimal amount,
		String currency, String paymentMethodCode, String paymentMethodName, String reference, Instant receivedAt,
		String recordedBy, BigDecimal totalPaid, BigDecimal balance) {
		static PaymentResponse from(EnrollmentPaymentReceipt payment) {
			return new PaymentResponse(payment.id(), payment.feeId(), payment.chargeId(), payment.receiptNumber(),
				payment.amount(), payment.currency(), payment.paymentMethodCode(), payment.paymentMethodName(),
				payment.reference(), payment.receivedAt(), payment.recordedBy(), payment.totalPaid(), payment.balance());
		}
	}
}
