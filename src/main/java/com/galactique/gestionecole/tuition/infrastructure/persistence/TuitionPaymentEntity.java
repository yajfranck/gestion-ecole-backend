package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.galactique.gestionecole.tuition.domain.StudentFeeCharge;
import com.galactique.gestionecole.tuition.domain.TuitionPayment;

@Entity
@Table(name = "tuition_payments")
public class TuitionPaymentEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(nullable = false)
	private UUID chargeId;
	@Column(nullable = false)
	private UUID establishmentId;
	@Column(nullable = false)
	private UUID studentId;
	@Column(nullable = false, unique = true, length = 32)
	private String receiptNumber;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;
	@Column(nullable = false, length = 3)
	private String currency;
	@Column(nullable = false, length = 40)
	private String paymentMethodCode;
	@Column(nullable = false, length = 100)
	private String paymentMethodName;
	@Column(length = 120)
	private String reference;
	@Column(nullable = false, updatable = false)
	private Instant receivedAt;
	@Column(nullable = false, updatable = false, length = 100)
	private String recordedBy;

	protected TuitionPaymentEntity() { }

	public TuitionPaymentEntity(StudentFeeCharge charge, String receiptNumber, BigDecimal amount,
		String paymentMethodCode, String paymentMethodName, String reference, String recordedBy) {
		chargeId = charge.id();
		establishmentId = charge.establishmentId();
		studentId = charge.studentId();
		this.receiptNumber = receiptNumber;
		this.amount = amount;
		currency = charge.currency();
		this.paymentMethodCode = paymentMethodCode;
		this.paymentMethodName = paymentMethodName;
		this.reference = reference;
		this.recordedBy = recordedBy;
	}

	@PrePersist
	void onCreate() { receivedAt = Instant.now(); }

	public TuitionPayment toDomain() {
		return new TuitionPayment(id, chargeId, establishmentId, studentId, receiptNumber,
			amount, currency, paymentMethodCode, paymentMethodName, reference, receivedAt, recordedBy);
	}
}