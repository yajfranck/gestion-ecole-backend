package com.galactique.gestionecole.tuition.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.galactique.gestionecole.tuition.domain.StudentFeeCharge;
import com.galactique.gestionecole.tuition.domain.TuitionPayment;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;

/** Shared recording rules; callers enforce their establishment scope and TUITION entitlement. */
@Service
public class TuitionPaymentRecorder {
	private final TuitionRepository repository;
	public TuitionPaymentRecorder(TuitionRepository repository) { this.repository = repository; }

	@Transactional
	public RecordedPayment record(UUID establishmentId, UUID studentId, UUID chargeId, BigDecimal amount,
		String paymentMethodCode, String reference, String recordedBy) {
		if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
			throw new TuitionValidationException("Amount must be positive and have at most two decimal places");
		}
		BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.UNNECESSARY);
		String normalizedMethod = required(paymentMethodCode, "paymentMethodCode", 40).toUpperCase(Locale.ROOT);
		var selectedMethod = repository.findPaymentMethods(establishmentId).stream()
			.filter(method -> method.code().equals(normalizedMethod)).findFirst()
			.orElseThrow(() -> new TuitionValidationException("The payment method is not enabled for this establishment"));
		StudentFeeCharge charge = repository.findChargeForUpdate(chargeId, establishmentId, studentId)
			.orElseThrow(TuitionNotFoundException::new);
		BigDecimal paid = repository.sumPayments(chargeId);
		if (normalizedAmount.compareTo(charge.amount().subtract(paid)) > 0) {
			throw new TuitionConflictException();
		}
		String normalizedReference = reference == null || reference.isBlank() ? null : required(reference, "reference", 120);
		String receiptNumber = String.format(Locale.ROOT, "REC-%08d", repository.nextReceiptNumber());
		TuitionPayment payment;
		try {
			payment = repository.savePayment(charge, receiptNumber, normalizedAmount, normalizedMethod,
				selectedMethod.name(), normalizedReference, recordedBy);
		} catch (DataIntegrityViolationException exception) {
			throw new TuitionConflictException();
		}
		BigDecimal updatedPaid = paid.add(normalizedAmount);
		return new RecordedPayment(payment, charge, updatedPaid, charge.amount().subtract(updatedPaid));
	}

	private String required(String value, String field, int maxLength) {
		if (value == null || value.isBlank() || value.strip().length() > maxLength) {
			throw new TuitionValidationException(field + " is required and must not exceed " + maxLength + " characters");
		}
		return value.strip();
	}

	public record RecordedPayment(TuitionPayment payment, StudentFeeCharge charge, BigDecimal totalPaid, BigDecimal balance) { }
}
