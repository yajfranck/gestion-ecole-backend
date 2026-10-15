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

import com.galactique.gestionecole.tuition.domain.StudentFeeCharge;
import com.galactique.gestionecole.tuition.domain.TuitionFee;

@Entity
@Table(name = "student_fee_charges")
public class StudentFeeChargeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(nullable = false)
	private UUID feeId;
	@Column(nullable = false)
	private UUID establishmentId;
	@Column(nullable = false)
	private UUID academicYearId;
	@Column(nullable = false)
	private UUID studentId;
	@Column(nullable = false, length = 40)
	private String studentNumber;
	@Column(nullable = false, length = 100)
	private String studentFirstName;
	@Column(nullable = false, length = 100)
	private String studentLastName;
	@Column(nullable = false, length = 120)
	private String name;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;
	@Column(nullable = false, length = 3)
	private String currency;
	@Column(nullable = false)
	private LocalDate dueDate;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected StudentFeeChargeEntity() { }

	public StudentFeeChargeEntity(TuitionFee fee, com.galactique.gestionecole.students.domain.Student student) {
		feeId = fee.id();
		establishmentId = fee.establishmentId();
		academicYearId = fee.academicYearId();
		studentId = student.id();
		studentNumber = student.studentNumber();
		studentFirstName = student.firstName();
		studentLastName = student.lastName();
		name = fee.name();
		amount = fee.amount();
		currency = fee.currency();
		dueDate = fee.dueDate();
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public StudentFeeCharge toDomain() {
		return new StudentFeeCharge(id, feeId, establishmentId, academicYearId, studentId,
			studentNumber, studentFirstName, studentLastName, name, amount, currency, dueDate, createdAt);
	}
}