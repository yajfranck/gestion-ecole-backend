package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
	private UUID schoolClassId;
	@ElementCollection
	@CollectionTable(name = "tuition_fee_classes", joinColumns = @JoinColumn(name = "fee_id"))
	private List<TuitionFeeClassTarget> schoolClasses = new ArrayList<>();
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
		schoolClassId = draft.schoolClassIds().size() == 1 ? draft.schoolClassIds().getFirst() : null;
		schoolClasses = draft.schoolClassIds().stream()
			.map(id -> new TuitionFeeClassTarget(draft.establishmentId(), draft.academicYearId(), id))
			.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		studentId = draft.studentId();
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public void addSchoolClasses(List<UUID> classIds) {
		if (schoolClasses.isEmpty()) { throw new IllegalArgumentException("An annual fee cannot be converted into a class fee"); }
		for (UUID classId : classIds) {
			if (schoolClasses.stream().noneMatch(target -> target.getSchoolClassId().equals(classId))) {
				schoolClasses.add(new TuitionFeeClassTarget(establishmentId, academicYearId, classId));
			}
		}
		schoolClassId = schoolClasses.size() == 1 ? schoolClasses.getFirst().getSchoolClassId() : null;
	}

	public TuitionFee toDomain() {
		return new TuitionFee(id, establishmentId, academicYearId, feeTypeId, feeTypeCode, feeTypeName,
			name, amount, currency,
			dueDate, schoolClasses.stream().map(TuitionFeeClassTarget::getSchoolClassId).sorted().toList(),
			studentId, enabled, createdAt);
	}
}
