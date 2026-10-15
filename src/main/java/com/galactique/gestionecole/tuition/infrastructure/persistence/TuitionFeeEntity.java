package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.galactique.gestionecole.tuition.domain.TuitionFee;
import com.galactique.gestionecole.tuition.domain.TuitionFeeDraft;

@Entity
@Table(name = "tuition_fee_definitions")
public class TuitionFeeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(nullable = false)
	private UUID establishmentId;
	@Column(nullable = false)
	private UUID academicYearId;
	@Column
	private UUID feeTypeId;
	@Column(length = 40)
	private String feeTypeCode;
	@Column(length = 100)
	private String feeTypeName;
	@Column(nullable = false, length = 120)
	private String name;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;
	@Column(nullable = false, length = 3)
	private String currency;
	@Column(nullable = false)
	private LocalDate dueDate;
	@Column
	private UUID gradeLevelId;
	@Column
	private UUID studentId;
	@Column(nullable = false)
	private boolean enabled = true;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected TuitionFeeEntity() { }

	public TuitionFeeEntity(TuitionFeeDraft draft) {
		establishmentId = draft.establishmentId();
		academicYearId = draft.academicYearId();
		feeTypeId = draft.feeTypeId();
		feeTypeCode = draft.feeTypeCode();
		feeTypeName = draft.feeTypeName();
		name = draft.name();
		amount = draft.amount();
		currency = draft.currency();
		dueDate = draft.dueDate();
		gradeLevelId = draft.gradeLevelId();
		studentId = draft.studentId();
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public TuitionFee toDomain() {
		return new TuitionFee(id, establishmentId, academicYearId, feeTypeId, feeTypeCode, feeTypeName,
			name, amount, currency,
			dueDate, gradeLevelId, studentId, enabled, createdAt);
	}
}