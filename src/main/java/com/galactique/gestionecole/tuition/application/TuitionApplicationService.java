package com.galactique.gestionecole.tuition.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.enrollments.application.EnrollmentAccessDeniedException;
import com.galactique.gestionecole.enrollments.application.EnrollmentApplicationService;
import com.galactique.gestionecole.enrollments.application.EnrollmentNotFoundException;
import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCalendarApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;
import com.galactique.gestionecole.students.application.StudentAccessDeniedException;
import com.galactique.gestionecole.students.application.StudentApplicationService;
import com.galactique.gestionecole.students.application.StudentNotFoundException;
import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.tuition.domain.StudentFeeCharge;
import com.galactique.gestionecole.tuition.domain.TuitionFee;
import com.galactique.gestionecole.tuition.domain.TuitionFeeDraft;
import com.galactique.gestionecole.tuition.domain.TuitionFeeType;
import com.galactique.gestionecole.tuition.domain.TuitionFeeTypeDraft;
import com.galactique.gestionecole.tuition.domain.TuitionPayment;
import com.galactique.gestionecole.tuition.domain.TuitionPaymentMethod;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;

@Service
public class TuitionApplicationService {
	private final TuitionRepository tuitionRepository;
	private final EmployeeApplicationService employeeApplicationService;
	private final StudentApplicationService studentApplicationService;
	private final SchoolCalendarApplicationService schoolCalendarApplicationService;
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;
	private final EnrollmentApplicationService enrollmentApplicationService;
	private final LicenseApplicationService licenseApplicationService;
	private final TuitionPaymentRecorder paymentRecorder;

	public TuitionApplicationService(TuitionRepository tuitionRepository,
		EmployeeApplicationService employeeApplicationService,
		StudentApplicationService studentApplicationService,
		SchoolCalendarApplicationService schoolCalendarApplicationService,
		SchoolCatalogApplicationService schoolCatalogApplicationService,
		EnrollmentApplicationService enrollmentApplicationService,
		LicenseApplicationService licenseApplicationService, TuitionPaymentRecorder paymentRecorder) {
		this.tuitionRepository = tuitionRepository;
		this.employeeApplicationService = employeeApplicationService;
		this.studentApplicationService = studentApplicationService;
		this.schoolCalendarApplicationService = schoolCalendarApplicationService;
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
		this.enrollmentApplicationService = enrollmentApplicationService;
		this.licenseApplicationService = licenseApplicationService;
		this.paymentRecorder = paymentRecorder;
	}

	@Transactional(readOnly = true)
	public List<TuitionFee> findFees(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		validateYear(establishmentId, academicYearId, actor);
		return tuitionRepository.findFees(establishmentId, academicYearId);
	}

	@Transactional
	public FeeCreationResult createFee(UUID establishmentId, UUID academicYearId,
		CreateTuitionFeeCommand command, EmployeeActor actor) {
		var year = validateYear(establishmentId, academicYearId, actor);
		try {
			year = schoolCalendarApplicationService.lockYearForEnrollment(establishmentId, academicYearId, actor);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new TuitionNotFoundException();
		} catch (AcademicCalendarAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
		String normalizedName = required(command.name(), "name", 120);
		BigDecimal normalizedAmount = normalizeAmount(command.amount());
		String normalizedCurrency = normalizeCurrency(command.currency());
		var feeType = tuitionRepository.findFeeType(command.feeTypeId(), establishmentId)
			.filter(TuitionFeeType::enabled).orElseThrow(TuitionNotFoundException::new);
		List<UUID> schoolClassIds = resolveSchoolClassIds(command);
		if (command.dueDate() == null || command.dueDate().isBefore(year.startsOn())
			|| command.dueDate().isAfter(year.endsOn())
			|| (!schoolClassIds.isEmpty() && command.studentId() != null)) {
			throw new TuitionValidationException("Due date must be within the academic year and a fee cannot target both classes and a student");
		}
		try {
			for (UUID schoolClassId : schoolClassIds) {
				schoolCatalogApplicationService.findClass(
					establishmentId, academicYearId, schoolClassId, actor);
			}
			if (command.studentId() != null) {
				studentApplicationService.findById(establishmentId, command.studentId(), actor);
			}
		} catch (SchoolCatalogNotFoundException | StudentNotFoundException exception) {
			throw new TuitionNotFoundException();
		} catch (SchoolCatalogAccessDeniedException | StudentAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}

		TuitionFee fee;
		try {
			fee = tuitionRepository.saveFee(new TuitionFeeDraft(establishmentId, academicYearId,
				feeType.id(), feeType.code(), feeType.name(), normalizedName, normalizedAmount, normalizedCurrency, command.dueDate(),
				schoolClassIds, command.studentId()));
		} catch (DataIntegrityViolationException exception) {
			throw new TuitionConflictException();
		}
		int createdCharges = applyFeeToCurrentEnrollments(fee, actor);
		if (command.studentId() != null && createdCharges == 0) {
			throw new TuitionValidationException("The selected student must have an active enrollment in this academic year");
		}
		return new FeeCreationResult(fee, createdCharges);
	}

	@Transactional
	public int applyFee(UUID establishmentId, UUID academicYearId, UUID feeId, EmployeeActor actor) {
		validateYear(establishmentId, academicYearId, actor);
		TuitionFee fee = tuitionRepository.findFee(feeId, establishmentId, academicYearId)
			.filter(TuitionFee::enabled).orElseThrow(TuitionNotFoundException::new);
		return applyFeeToCurrentEnrollments(fee, actor);
	}

	@Transactional(readOnly = true)
	public List<ChargeBalance> findStudentCharges(UUID establishmentId, UUID academicYearId, UUID studentId, EmployeeActor actor) {
		validateYear(establishmentId, academicYearId, actor);
		validateStudent(establishmentId, studentId, actor);
		return tuitionRepository.findChargesForStudentAndYear(establishmentId, studentId, academicYearId).stream()
			.map(this::toBalance).toList();
	}

	@Transactional(readOnly = true)
	public List<ChargeBalance> findStudentChargesForGuardian(UUID establishmentId, UUID studentId,
		UUID academicYearId) {
		requireTuitionLicense(establishmentId);
		List<StudentFeeCharge> charges = tuitionRepository.findChargesForStudentAndYear(
			establishmentId, studentId, academicYearId);
		Map<UUID, List<TuitionPayment>> paymentsByCharge = new java.util.HashMap<>();
		for (TuitionPayment payment : tuitionRepository.findPaymentsForCharges(
			charges.stream().map(StudentFeeCharge::id).toList())) {
			paymentsByCharge.computeIfAbsent(payment.chargeId(), ignored -> new ArrayList<>()).add(payment);
		}
		return charges.stream().map(charge -> toBalance(charge,
			paymentsByCharge.getOrDefault(charge.id(), List.of()))).toList();
	}

	@Transactional(readOnly = true)
	public List<TuitionPayment> findPaymentsForGuardian(UUID establishmentId, UUID studentId,
		UUID academicYearId, UUID chargeId, LocalDate from, LocalDate to) {
		requireTuitionLicense(establishmentId);
		if (from == null || to == null || to.isBefore(from)
			|| java.time.temporal.ChronoUnit.DAYS.between(from, to) >= 366) {
			throw new TuitionValidationException("Payment history range must be valid and not exceed 366 days");
		}
		tuitionRepository.findCharge(chargeId, establishmentId, studentId, academicYearId)
			.orElseThrow(TuitionNotFoundException::new);
		java.time.Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
		java.time.Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
		return tuitionRepository.findPaymentsForChargeBetween(chargeId, establishmentId, studentId, start, end);
	}

	@Transactional(readOnly = true)
	public List<ChargeBalance> findOutstanding(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		validateYear(establishmentId, academicYearId, actor);
		return tuitionRepository.findChargesForYear(establishmentId, academicYearId).stream()
			.map(this::toBalance).filter(balance -> balance.balance().signum() > 0).toList();
	}

	@Transactional
	public PaymentResult recordPayment(UUID establishmentId, UUID academicYearId, UUID studentId, UUID chargeId,
		BigDecimal amount, String paymentMethodCode, String reference, EmployeeActor actor) {
		validateYear(establishmentId, academicYearId, actor);
		validateStudent(establishmentId, studentId, actor);
		tuitionRepository.findCharge(chargeId, establishmentId, studentId, academicYearId).orElseThrow(TuitionNotFoundException::new);
		var result = paymentRecorder.record(establishmentId, studentId, chargeId, amount,
			paymentMethodCode, reference, actor.username());
		return new PaymentResult(result.payment(), result.charge(), result.totalPaid(), result.balance());
	}

	@Transactional(readOnly = true)
	public Receipt findReceipt(UUID establishmentId, UUID academicYearId, UUID paymentId, EmployeeActor actor) {
		validateYear(establishmentId, academicYearId, actor);
		TuitionPayment payment = tuitionRepository.findPayment(paymentId, establishmentId, academicYearId)
			.orElseThrow(TuitionNotFoundException::new);
		StudentFeeCharge charge = tuitionRepository.findChargesForStudentAndYear(establishmentId, payment.studentId(), academicYearId).stream()
			.filter(item -> item.id().equals(payment.chargeId())).findFirst().orElseThrow(TuitionNotFoundException::new);
		BigDecimal paidThroughReceipt = BigDecimal.ZERO;
		for (TuitionPayment recordedPayment : tuitionRepository.findPayments(charge.id()).stream()
			.sorted(java.util.Comparator.comparing(transaction -> transaction.receiptNumber())).toList()) {
			paidThroughReceipt = paidThroughReceipt.add(recordedPayment.amount());
			if (recordedPayment.id().equals(payment.id())) {
				break;
			}
		}
		return new Receipt(payment, charge, charge.amount().subtract(paidThroughReceipt));
	}

	@Transactional(readOnly = true)
	public List<TuitionPaymentMethod> findPaymentMethods(UUID establishmentId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return tuitionRepository.findPaymentMethods(establishmentId);
	}

	@Transactional
	public TuitionPaymentMethod renamePaymentMethod(UUID establishmentId, String code, String name, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		String normalizedCode = required(code, "code", 40).toUpperCase(Locale.ROOT);
		String normalizedName = required(name, "name", 100);
		return tuitionRepository.updatePaymentMethodLabel(establishmentId, normalizedCode, normalizedName)
			.orElseThrow(TuitionNotFoundException::new);
	}

	@Transactional
	public void deletePaymentMethod(UUID establishmentId, String code, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		String normalizedCode = required(code, "code", 40).toUpperCase(Locale.ROOT);
		if (!tuitionRepository.disablePaymentMethod(establishmentId, normalizedCode)) {
			throw new TuitionNotFoundException();
		}
	}

	@Transactional(readOnly = true)
	public List<TuitionFeeType> findFeeTypes(UUID establishmentId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return tuitionRepository.findFeeTypes(establishmentId);
	}

	@Transactional
	public TuitionFeeType createFeeType(UUID establishmentId, String code, String name, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		String normalizedCode = required(code, "code", 40).toUpperCase(Locale.ROOT);
		String normalizedName = required(name, "name", 100);
		if (!normalizedCode.matches("[A-Z][A-Z0-9_]*")) {
			throw new TuitionValidationException("Fee type code must use uppercase letters, numbers, and underscores");
		}
		if (tuitionRepository.feeTypeCodeExists(establishmentId, normalizedCode)
			|| tuitionRepository.feeTypeNameExists(establishmentId, normalizedName)) {
			throw new TuitionConflictException();
		}
		try {
			return tuitionRepository.saveFeeType(new TuitionFeeTypeDraft(establishmentId, normalizedCode, normalizedName));
		} catch (DataIntegrityViolationException exception) {
			throw new TuitionConflictException();
		}
	}

	@Transactional
	public TuitionFeeType renameFeeType(UUID establishmentId, UUID feeTypeId, String name, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		TuitionFeeType current = tuitionRepository.findFeeType(feeTypeId, establishmentId)
			.filter(TuitionFeeType::enabled).orElseThrow(TuitionNotFoundException::new);
		String normalizedName = required(name, "name", 100);
		if (tuitionRepository.feeTypeNameExistsExcept(establishmentId, normalizedName, feeTypeId)) {
			throw new TuitionConflictException();
		}
		try {
			return tuitionRepository.updateFeeTypeName(current.id(), establishmentId, normalizedName);
		} catch (DataIntegrityViolationException exception) {
			throw new TuitionConflictException();
		}
	}

	@Transactional
	public void deleteFeeType(UUID establishmentId, UUID feeTypeId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		if (tuitionRepository.findFeeType(feeTypeId, establishmentId).filter(TuitionFeeType::enabled).isEmpty()
			|| !tuitionRepository.disableFeeType(feeTypeId, establishmentId)) {
			throw new TuitionNotFoundException();
		}
	}

	@Transactional
	public List<TuitionPaymentMethod> replacePaymentMethods(UUID establishmentId,
		List<PaymentMethodInput> inputs, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		if (inputs == null || inputs.isEmpty()) {
			throw new TuitionValidationException("At least one payment method is required");
		}
		List<TuitionPaymentMethod> methods = new ArrayList<>();
		for (PaymentMethodInput input : inputs) {
			String code = required(input.code(), "code", 40).toUpperCase(Locale.ROOT);
			if (!code.matches("[A-Z][A-Z0-9_]*")) {
				throw new TuitionValidationException("Payment method code must contain uppercase letters, numbers, or underscores");
			}
			String name = required(input.name(), "name", 100);
			if (methods.stream().anyMatch(method -> method.code().equals(code))) {
				throw new TuitionValidationException("Payment method codes must be unique");
			}
			methods.add(new TuitionPaymentMethod(UUID.randomUUID(), establishmentId, code, name, true));
		}
		tuitionRepository.replacePaymentMethods(establishmentId, methods);
		return methods;
	}

	private int applyFeeToCurrentEnrollments(TuitionFee fee, EmployeeActor actor) {
		int count = 0;
		for (Enrollment enrollment : findEnrollments(fee, actor)) {
			if (matchesFee(fee, enrollment, actor) && createChargeIfAbsent(fee, enrollment, actor)) {
				count++;
			}
		}
		return count;
	}

	private List<Enrollment> findEnrollments(TuitionFee fee, EmployeeActor actor) {
		try {
			return enrollmentApplicationService.findAll(fee.establishmentId(), fee.academicYearId(), actor);
		} catch (EnrollmentNotFoundException | AcademicCalendarNotFoundException exception) {
			throw new TuitionNotFoundException();
		} catch (EnrollmentAccessDeniedException | AcademicCalendarAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
	}

	private boolean matchesFee(TuitionFee fee, Enrollment enrollment, EmployeeActor actor) {
		if (fee.studentId() != null && !fee.studentId().equals(enrollment.studentId())) {
			return false;
		}
		if (fee.schoolClassIds().isEmpty()) {
			return true;
		}
		if (!fee.schoolClassIds().contains(enrollment.schoolClassId())) {
			return false;
		}
		try {
			SchoolClass schoolClass = schoolCatalogApplicationService.findClass(fee.establishmentId(),
				fee.academicYearId(), enrollment.schoolClassId(), actor);
			return fee.schoolClassIds().contains(schoolClass.id());
		} catch (SchoolCatalogNotFoundException exception) {
			return false;
		} catch (SchoolCatalogAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
	}

	private boolean createChargeIfAbsent(TuitionFee fee, Enrollment enrollment, EmployeeActor actor) {
		if (tuitionRepository.existsCharge(fee.id(), enrollment.studentId())) {
			return false;
		}
		try {
			Student student = studentApplicationService.findById(fee.establishmentId(), enrollment.studentId(), actor);
			tuitionRepository.saveCharge(fee, student);
			return true;
		} catch (StudentNotFoundException exception) {
			return false;
		} catch (StudentAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
	}

	private ChargeBalance toBalance(StudentFeeCharge charge) {
		List<TuitionPayment> payments = tuitionRepository.findPayments(charge.id());
		return toBalance(charge, payments);
	}

	private ChargeBalance toBalance(StudentFeeCharge charge, List<TuitionPayment> payments) {
		BigDecimal paid = payments.stream().map(TuitionPayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new ChargeBalance(charge, paid, charge.amount().subtract(paid),
			charge.dueDate().isBefore(LocalDate.now(ZoneOffset.UTC)) && charge.amount().compareTo(paid) > 0, payments);
	}

	private void validateStudent(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		try {
			studentApplicationService.findById(establishmentId, studentId, actor);
		} catch (StudentNotFoundException exception) {
			throw new TuitionNotFoundException();
		} catch (StudentAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
	}

	private AcademicYear validateYear(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		try {
			return schoolCalendarApplicationService.findYear(establishmentId, academicYearId, actor);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new TuitionNotFoundException();
		} catch (AcademicCalendarAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
	}

	private void requireEstablishment(UUID establishmentId, EmployeeActor actor) {
		requireTuitionLicense(establishmentId);
		try {
			boolean accessible = employeeApplicationService.findEstablishments(actor).stream()
				.anyMatch(establishment -> establishment.enabled() && establishment.id().equals(establishmentId));
			if (!accessible) {
				throw new TuitionNotFoundException();
			}
		} catch (EmployeeAccessDeniedException exception) {
			throw new TuitionAccessDeniedException();
		}
	}

	private void requireTuitionLicense(UUID establishmentId) {
		if (!licenseApplicationService.canAccessFeature(establishmentId, LicenseFeature.TUITION,
			java.time.Instant.now())) {
			throw new TuitionAccessDeniedException();
		}
	}

	private List<UUID> resolveSchoolClassIds(CreateTuitionFeeCommand command) {
		if (command.schoolClassIds() == null) {
			return command.schoolClassId() == null ? List.of() : List.of(command.schoolClassId());
		}
		List<UUID> ids = command.schoolClassIds();
		if (command.schoolClassId() != null || ids.isEmpty() || ids.stream().anyMatch(java.util.Objects::isNull)
			|| new java.util.HashSet<>(ids).size() != ids.size()) {
			throw new TuitionValidationException("Provide either schoolClassId or a non-empty schoolClassIds list with unique non-null IDs");
		}
		return List.copyOf(ids);
	}

	private BigDecimal normalizeAmount(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
			throw new TuitionValidationException("Amount must be positive and have at most two decimal places");
		}
		try {
			return amount.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new TuitionValidationException("Amount must have at most two decimal places");
		}
	}

	private String normalizeCurrency(String currency) {
		String normalized = required(currency, "currency", 3).toUpperCase(Locale.ROOT);
		if (!normalized.matches("[A-Z]{3}")) {
			throw new TuitionValidationException("Currency must be a three-letter code");
		}
		return normalized;
	}

	private String required(String value, String field, int maxLength) {
		if (value == null || value.isBlank() || value.strip().length() > maxLength) {
			throw new TuitionValidationException(field + " is required and must not exceed " + maxLength + " characters");
		}
		return value.strip();
	}

	public record FeeCreationResult(TuitionFee fee, int chargesCreated) { }
	public record PaymentMethodInput(String code, String name) { }
	public record ChargeBalance(StudentFeeCharge charge, BigDecimal paid, BigDecimal balance,
		boolean overdue, List<TuitionPayment> payments) { }
	public record PaymentResult(TuitionPayment payment, StudentFeeCharge charge,
		BigDecimal totalPaid, BigDecimal balance) { }
	public record Receipt(TuitionPayment payment, StudentFeeCharge charge, BigDecimal balanceAfterPayment) { }
}
