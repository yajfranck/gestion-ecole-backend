package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.enrollments.application.EnrollmentFeeChargeGateway;
import com.galactique.gestionecole.enrollments.application.EnrollmentAccessDeniedException;
import com.galactique.gestionecole.enrollments.application.EnrollmentNotFoundException;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentConflictException;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentInput;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentReceipt;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentRequiredException;
import com.galactique.gestionecole.enrollments.application.EnrollmentPaymentValidationException;
import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.tuition.application.TuitionConflictException;
import com.galactique.gestionecole.tuition.application.TuitionNotFoundException;
import com.galactique.gestionecole.tuition.application.TuitionPaymentRecorder;
import com.galactique.gestionecole.tuition.application.TuitionValidationException;
import com.galactique.gestionecole.tuition.domain.StudentFeeCharge;
import com.galactique.gestionecole.tuition.domain.TuitionFee;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;

@Component
public class JpaEnrollmentFeeChargeGateway implements EnrollmentFeeChargeGateway {
	private final TuitionRepository tuitionRepository;
	private final StudentRepository studentRepository;
	private final TuitionPaymentRecorder paymentRecorder;
	private final LicenseApplicationService licenseApplicationService;

	public JpaEnrollmentFeeChargeGateway(TuitionRepository tuitionRepository,
		StudentRepository studentRepository, TuitionPaymentRecorder paymentRecorder,
		LicenseApplicationService licenseApplicationService) {
		this.tuitionRepository = tuitionRepository;
		this.studentRepository = studentRepository;
		this.paymentRecorder = paymentRecorder;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Override
	@Transactional
	public List<EnrollmentPaymentReceipt> applyFeesAndRecordPayments(Enrollment enrollment,
		List<EnrollmentPaymentInput> payments, EmployeeActor actor) {
		List<TuitionFee> fees = tuitionRepository.findFees(enrollment.establishmentId(), enrollment.academicYearId())
			.stream().filter(TuitionFee::enabled)
			.filter(fee -> fee.studentId() == null || fee.studentId().equals(enrollment.studentId()))
			.filter(fee -> fee.schoolClassIds().isEmpty() || fee.schoolClassIds().contains(enrollment.schoolClassId())).toList();
		if (fees.isEmpty()) {
			if (!payments.isEmpty()) { throw new EnrollmentNotFoundException(); }
			return List.of();
		}
		if (!licenseApplicationService.canAccessFeature(enrollment.establishmentId(), LicenseFeature.TUITION,
			java.time.Instant.now())) {
			throw new EnrollmentAccessDeniedException();
		}
		HashSet<UUID> requestedFees = new HashSet<>();
		var applicableIds = fees.stream().map(TuitionFee::id).collect(Collectors.toSet());
		for (EnrollmentPaymentInput payment : payments) {
			if (payment == null || payment.feeId() == null || !requestedFees.add(payment.feeId())) {
				throw new EnrollmentPaymentValidationException("Payment fee IDs must be non-null and unique");
			}
			if (!applicableIds.contains(payment.feeId())) { throw new EnrollmentNotFoundException(); }
		}
		var student = studentRepository.findByIdAndEstablishmentId(
			enrollment.studentId(), enrollment.establishmentId()).orElseThrow();
		for (TuitionFee fee : fees) {
			if (!tuitionRepository.existsCharge(fee.id(), enrollment.studentId())) {
				tuitionRepository.saveCharge(fee, student);
			}
		}
		var chargesByFee = tuitionRepository.findChargesForStudentAndYear(enrollment.establishmentId(),
			enrollment.studentId(), enrollment.academicYearId()).stream()
			.filter(charge -> applicableIds.contains(charge.feeId()))
			.collect(Collectors.toMap(StudentFeeCharge::feeId, Function.identity()));
		List<EnrollmentPaymentReceipt> receipts = new ArrayList<>();
		try {
			for (EnrollmentPaymentInput input : payments) {
				var charge = chargesByFee.get(input.feeId());
				var result = paymentRecorder.record(enrollment.establishmentId(), enrollment.studentId(), charge.id(),
					input.amount(), input.paymentMethodCode(), input.reference(), actor.username());
				var payment = result.payment();
				receipts.add(new EnrollmentPaymentReceipt(payment.id(), input.feeId(), charge.id(), payment.receiptNumber(),
					payment.amount(), payment.currency(), payment.paymentMethodCode(), payment.paymentMethodName(),
					payment.reference(), payment.receivedAt(), payment.recordedBy(), result.totalPaid(), result.balance()));
			}
		} catch (TuitionValidationException exception) {
			throw new EnrollmentPaymentValidationException(exception.getMessage());
		} catch (TuitionNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (TuitionConflictException exception) {
			throw new EnrollmentPaymentConflictException();
		}
		boolean hasPayment = false;
		List<UUID> unpaidFees = new ArrayList<>();
		for (StudentFeeCharge charge : chargesByFee.values()) {
			var paid = tuitionRepository.sumPayments(charge.id());
			hasPayment |= paid.signum() > 0;
			if (paid.compareTo(charge.amount()) < 0) { unpaidFees.add(charge.feeId()); }
		}
		if (!hasPayment || !enrollment.advanceAccepted() && !unpaidFees.isEmpty()) {
			throw new EnrollmentPaymentRequiredException(unpaidFees);
		}
		return List.copyOf(receipts);
	}
}
