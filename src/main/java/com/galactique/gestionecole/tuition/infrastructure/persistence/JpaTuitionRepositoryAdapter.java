package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.tuition.domain.StudentFeeCharge;
import com.galactique.gestionecole.tuition.domain.TuitionFee;
import com.galactique.gestionecole.tuition.domain.TuitionPayment;
import com.galactique.gestionecole.tuition.domain.TuitionPaymentMethod;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;
import com.galactique.gestionecole.tuition.domain.TuitionFeeDraft;
import com.galactique.gestionecole.tuition.domain.TuitionFeeType;
import com.galactique.gestionecole.tuition.domain.TuitionFeeTypeDraft;
import com.galactique.gestionecole.students.domain.Student;

@Repository
public class JpaTuitionRepositoryAdapter implements TuitionRepository {
	private final TuitionFeeJpaRepository feeRepository;
	private final StudentFeeChargeJpaRepository chargeRepository;
	private final TuitionPaymentJpaRepository paymentRepository;
	private final TuitionPaymentMethodJpaRepository paymentMethodRepository;
	private final TuitionFeeTypeJpaRepository feeTypeRepository;

	public JpaTuitionRepositoryAdapter(TuitionFeeJpaRepository feeRepository,
		StudentFeeChargeJpaRepository chargeRepository, TuitionPaymentJpaRepository paymentRepository,
		TuitionPaymentMethodJpaRepository paymentMethodRepository,
		TuitionFeeTypeJpaRepository feeTypeRepository) {
		this.feeRepository = feeRepository;
		this.chargeRepository = chargeRepository;
		this.paymentRepository = paymentRepository;
		this.paymentMethodRepository = paymentMethodRepository;
		this.feeTypeRepository = feeTypeRepository;
	}

	@Override
	@Transactional
	public TuitionFee saveFee(TuitionFeeDraft draft) {
		return feeRepository.saveAndFlush(new TuitionFeeEntity(draft)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TuitionFee> findFees(UUID establishmentId, UUID academicYearId) {
		return feeRepository.findAllByEstablishmentIdAndAcademicYearIdOrderByCreatedAtAsc(
			establishmentId, academicYearId).stream().map(TuitionFeeEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<TuitionFee> findFee(UUID feeId, UUID establishmentId, UUID academicYearId) {
		return feeRepository.findByIdAndEstablishmentIdAndAcademicYearId(feeId, establishmentId, academicYearId)
			.map(TuitionFeeEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsCharge(UUID feeId, UUID studentId) {
		return chargeRepository.existsByFeeIdAndStudentId(feeId, studentId);
	}

	@Override
	@Transactional
	public TuitionFee extendFeeClasses(UUID feeId, UUID establishmentId, UUID academicYearId, List<UUID> schoolClassIds) {
		var entity = feeRepository.findByIdAndEstablishmentIdAndAcademicYearId(feeId, establishmentId, academicYearId).orElseThrow();
		entity.addSchoolClasses(schoolClassIds);
		return feeRepository.saveAndFlush(entity).toDomain();
	}

	@Override
	@Transactional
	public StudentFeeCharge saveCharge(TuitionFee fee, Student student) {
		return chargeRepository.saveAndFlush(new StudentFeeChargeEntity(fee, student)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudentFeeCharge> findChargesForStudent(UUID establishmentId, UUID studentId) {
		return chargeRepository.findAllByEstablishmentIdAndStudentIdOrderByDueDateAsc(establishmentId, studentId)
			.stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudentFeeCharge> findChargesForStudentAndYear(
		UUID establishmentId, UUID studentId, UUID academicYearId) {
		return chargeRepository.findAllByEstablishmentIdAndStudentIdAndAcademicYearIdOrderByDueDateAsc(
			establishmentId, studentId, academicYearId).stream().map(StudentFeeChargeEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StudentFeeCharge> findChargesForYear(UUID establishmentId, UUID academicYearId) {
		return chargeRepository.findAllByEstablishmentIdAndAcademicYearIdOrderByDueDateAsc(establishmentId, academicYearId)
			.stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StudentFeeCharge> findCharge(UUID chargeId, UUID establishmentId,
		UUID studentId, UUID academicYearId) {
		return chargeRepository.findByIdAndEstablishmentIdAndStudentIdAndAcademicYearId(
			chargeId, establishmentId, studentId, academicYearId).map(StudentFeeChargeEntity::toDomain);
	}

	@Override
	@Transactional
	public Optional<StudentFeeCharge> findChargeForUpdate(UUID chargeId, UUID establishmentId, UUID studentId) {
		return chargeRepository.findByIdForUpdate(chargeId, establishmentId, studentId)
			.map(entity -> entity.toDomain());
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal sumPayments(UUID chargeId) {
		BigDecimal total = paymentRepository.sumAmountByChargeId(chargeId);
		return total == null ? BigDecimal.ZERO : total;
	}

	@Override
	@Transactional(readOnly = true)
	public List<TuitionPayment> findPayments(UUID chargeId) {
		return paymentRepository.findAllByChargeIdOrderByReceivedAtAsc(chargeId)
			.stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TuitionPayment> findPaymentsForCharges(List<UUID> chargeIds) {
		if (chargeIds.isEmpty()) {
			return List.of();
		}
		return paymentRepository.findAllByChargeIdInOrderByReceivedAtAsc(chargeIds)
			.stream().map(TuitionPaymentEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TuitionPayment> findPaymentsForChargeBetween(UUID chargeId, UUID establishmentId,
		UUID studentId, java.time.Instant from, java.time.Instant to) {
		return paymentRepository.findAllByChargeIdAndEstablishmentIdAndStudentIdAndReceivedAtBetweenOrderByReceivedAtAsc(
			chargeId, establishmentId, studentId, from, to).stream().map(TuitionPaymentEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<TuitionPayment> findPayment(UUID paymentId, UUID establishmentId) {
		return paymentRepository.findByIdAndEstablishmentId(paymentId, establishmentId)
			.map(entity -> entity.toDomain());
	}

	@Override
	@Transactional
	public TuitionPayment savePayment(StudentFeeCharge charge, String receiptNumber, BigDecimal amount,
		String paymentMethodCode, String paymentMethodName, String reference, String recordedBy) {
		return paymentRepository.saveAndFlush(new TuitionPaymentEntity(charge, receiptNumber, amount,
			paymentMethodCode, paymentMethodName, reference, recordedBy)).toDomain();
	}

	@Override
	@Transactional
	public long nextReceiptNumber() { return paymentRepository.nextReceiptNumber(); }

	@Override
	@Transactional(readOnly = true)
	public List<TuitionPaymentMethod> findPaymentMethods(UUID establishmentId) {
		return paymentMethodRepository.findAllByEstablishmentIdAndEnabledTrueOrderByNameAsc(establishmentId)
			.stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TuitionPaymentMethod> findAllPaymentMethods(UUID establishmentId) {
		return paymentMethodRepository.findAllByEstablishmentIdOrderByNameAsc(establishmentId)
			.stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasPaymentMethod(UUID establishmentId, String code) {
		return paymentMethodRepository.existsByEstablishmentIdAndCodeAndEnabledTrue(establishmentId, code);
	}

	@Override
	@Transactional
	public Optional<TuitionPaymentMethod> updatePaymentMethodLabel(UUID establishmentId, String code, String name) {
		return paymentMethodRepository.findByEstablishmentIdAndCodeAndEnabledTrue(establishmentId, code)
			.map(entity -> {
				entity.updateName(name);
				return entity.toDomain();
			});
	}

	@Override
	@Transactional
	public boolean disablePaymentMethod(UUID establishmentId, String code) {
		return paymentMethodRepository.findByEstablishmentIdAndCodeAndEnabledTrue(establishmentId, code)
			.map(entity -> {
				entity.disable();
				return true;
			}).orElse(false);
	}

	@Override
	@Transactional
	public void replacePaymentMethods(UUID establishmentId, List<TuitionPaymentMethod> methods) {
		Map<String, TuitionPaymentMethod> requested = new LinkedHashMap<>();
		methods.forEach(method -> requested.put(method.code(), method));
		List<TuitionPaymentMethodEntity> additions = new java.util.ArrayList<>();
		for (TuitionPaymentMethodEntity existing : paymentMethodRepository
			.findAllByEstablishmentIdOrderByNameAsc(establishmentId)) {
			TuitionPaymentMethod replacement = requested.remove(existing.getCode());
			if (replacement == null) {
				existing.disable();
			} else {
				existing.updateName(replacement.name());
				existing.enable();
			}
		}
		requested.values().stream().map(TuitionPaymentMethodEntity::new).forEach(additions::add);
		paymentMethodRepository.saveAll(additions);
	}

	@Override
	@Transactional
	public TuitionFeeType saveFeeType(TuitionFeeTypeDraft draft) {
		return feeTypeRepository.saveAndFlush(new TuitionFeeTypeEntity(draft)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TuitionFeeType> findFeeTypes(UUID establishmentId) {
		return feeTypeRepository.findAllByEstablishmentIdAndEnabledTrueOrderByNameAsc(establishmentId)
			.stream().map(entity -> entity.toDomain()).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<TuitionFeeType> findFeeType(UUID id, UUID establishmentId) {
		return feeTypeRepository.findByIdAndEstablishmentId(id, establishmentId)
			.filter(entity -> entity.isEnabled()).map(entity -> entity.toDomain());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean feeTypeCodeExists(UUID establishmentId, String code) {
		return feeTypeRepository.existsByEstablishmentIdAndCodeIgnoreCase(establishmentId, code);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean feeTypeNameExists(UUID establishmentId, String name) {
		return feeTypeRepository.existsByEstablishmentIdAndNameIgnoreCase(establishmentId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean feeTypeNameExistsExcept(UUID establishmentId, String name, UUID id) {
		return feeTypeRepository.existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(establishmentId, name, id);
	}

	@Override
	@Transactional
	public TuitionFeeType updateFeeTypeName(UUID id, UUID establishmentId, String name) {
		TuitionFeeTypeEntity entity = feeTypeRepository.findByIdAndEstablishmentId(id, establishmentId)
			.filter(existing -> existing.isEnabled()).orElseThrow();
		entity.updateName(name);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public boolean disableFeeType(UUID id, UUID establishmentId) {
		return feeTypeRepository.findByIdAndEstablishmentId(id, establishmentId)
			.filter(entity -> entity.isEnabled())
			.map(entity -> {
				entity.disable();
				return true;
			}).orElse(false);
	}
}
