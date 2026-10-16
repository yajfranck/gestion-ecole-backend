package com.galactique.gestionecole.tuition.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galactique.gestionecole.students.domain.Student;

public interface TuitionRepository {
	TuitionFee saveFee(TuitionFeeDraft draft);
	List<TuitionFee> findFees(UUID establishmentId, UUID academicYearId);
	Optional<TuitionFee> findFee(UUID feeId, UUID establishmentId, UUID academicYearId);
	boolean existsCharge(UUID feeId, UUID studentId);
	StudentFeeCharge saveCharge(TuitionFee fee, Student student);
	List<StudentFeeCharge> findChargesForStudent(UUID establishmentId, UUID studentId);
	List<StudentFeeCharge> findChargesForStudentAndYear(UUID establishmentId, UUID studentId, UUID academicYearId);
	List<StudentFeeCharge> findChargesForYear(UUID establishmentId, UUID academicYearId);
	Optional<StudentFeeCharge> findCharge(UUID chargeId, UUID establishmentId, UUID studentId, UUID academicYearId);
	Optional<StudentFeeCharge> findChargeForUpdate(UUID chargeId, UUID establishmentId, UUID studentId);
	BigDecimal sumPayments(UUID chargeId);
	List<TuitionPayment> findPayments(UUID chargeId);
	List<TuitionPayment> findPaymentsForCharges(List<UUID> chargeIds);
	List<TuitionPayment> findPaymentsForChargeBetween(UUID chargeId, UUID establishmentId, UUID studentId,
		java.time.Instant from, java.time.Instant to);
	Optional<TuitionPayment> findPayment(UUID paymentId, UUID establishmentId);
	TuitionPayment savePayment(StudentFeeCharge charge, String receiptNumber, BigDecimal amount,
		String paymentMethodCode, String paymentMethodName, String reference, String recordedBy);
	long nextReceiptNumber();
	List<TuitionPaymentMethod> findPaymentMethods(UUID establishmentId);
	List<TuitionPaymentMethod> findAllPaymentMethods(UUID establishmentId);
	boolean hasPaymentMethod(UUID establishmentId, String code);
	Optional<TuitionPaymentMethod> updatePaymentMethodLabel(UUID establishmentId, String code, String name);
	boolean disablePaymentMethod(UUID establishmentId, String code);
	void replacePaymentMethods(UUID establishmentId, List<TuitionPaymentMethod> methods);
	TuitionFeeType saveFeeType(TuitionFeeTypeDraft draft);
	List<TuitionFeeType> findFeeTypes(UUID establishmentId);
	Optional<TuitionFeeType> findFeeType(UUID id, UUID establishmentId);
	boolean feeTypeCodeExists(UUID establishmentId, String code);
	boolean feeTypeNameExists(UUID establishmentId, String name);
	boolean feeTypeNameExistsExcept(UUID establishmentId, String name, UUID id);
	TuitionFeeType updateFeeTypeName(UUID id, UUID establishmentId, String name);
	boolean disableFeeType(UUID id, UUID establishmentId);
}