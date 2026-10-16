package com.galactique.gestionecole.guardians.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.attendance.application.AttendanceApplicationService;
import com.galactique.gestionecole.attendance.application.AttendanceHistory;
import com.galactique.gestionecole.attendance.application.AttendanceNotFoundException;
import com.galactique.gestionecole.attendance.application.AttendanceValidationException;
import com.galactique.gestionecole.enrollments.application.EnrollmentApplicationService;
import com.galactique.gestionecole.enrollments.application.EnrollmentNotFoundException;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCalendarApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;
import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.learning.application.LearningContentApplicationService;
import com.galactique.gestionecole.learning.domain.LearningAttachment;
import com.galactique.gestionecole.learning.domain.LearningContent;
import com.galactique.gestionecole.learning.domain.LearningContentPage;
import com.galactique.gestionecole.learning.domain.LearningContentType;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.tuition.application.TuitionApplicationService;
import com.galactique.gestionecole.tuition.application.TuitionApplicationService.ChargeBalance;
import com.galactique.gestionecole.tuition.application.TuitionNotFoundException;
import com.galactique.gestionecole.tuition.application.TuitionValidationException;
import com.galactique.gestionecole.tuition.domain.TuitionPayment;

@Service
public class GuardianChildPortalApplicationService {
	private final GuardianApplicationService guardianApplicationService;
	private final EstablishmentRepository establishmentRepository;
	private final SchoolCalendarApplicationService schoolCalendarApplicationService;
	private final EnrollmentApplicationService enrollmentApplicationService;
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;
	private final TuitionApplicationService tuitionApplicationService;
	private final AttendanceApplicationService attendanceApplicationService;
	private final LearningContentApplicationService learningContentApplicationService;
	private final LicenseApplicationService licenseApplicationService;

	public GuardianChildPortalApplicationService(GuardianApplicationService guardianApplicationService,
		EstablishmentRepository establishmentRepository,
		SchoolCalendarApplicationService schoolCalendarApplicationService,
		EnrollmentApplicationService enrollmentApplicationService,
		SchoolCatalogApplicationService schoolCatalogApplicationService,
		TuitionApplicationService tuitionApplicationService,
		AttendanceApplicationService attendanceApplicationService,
		LearningContentApplicationService learningContentApplicationService,
		LicenseApplicationService licenseApplicationService) {
		this.guardianApplicationService = guardianApplicationService;
		this.establishmentRepository = establishmentRepository;
		this.schoolCalendarApplicationService = schoolCalendarApplicationService;
		this.enrollmentApplicationService = enrollmentApplicationService;
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
		this.tuitionApplicationService = tuitionApplicationService;
		this.attendanceApplicationService = attendanceApplicationService;
		this.learningContentApplicationService = learningContentApplicationService;
		this.licenseApplicationService = licenseApplicationService;
	}
	@Transactional(readOnly = true)
	public ChildSummary findSummary(String username, UUID studentId, UUID academicYearId) {
		Student student = findLinkedChild(username, studentId);
		var establishment = establishmentRepository.findById(student.establishmentId())
			.filter(Establishment::enabled).orElseThrow(GuardianNotFoundException::new);
		AcademicYear year = findYear(student.establishmentId(), academicYearId);
		var enrollment = findEnrollment(student.establishmentId(), academicYearId, studentId);
		SchoolClass schoolClass = findClass(student.establishmentId(), academicYearId, enrollment.schoolClassId());
		List<ChargeBalance> charges = tuitionApplicationService.findStudentChargesForGuardian(
			student.establishmentId(), studentId, academicYearId);
		Map<String, CurrencyBalanceAccumulator> byCurrency = new TreeMap<>();
		for (ChargeBalance charge : charges) {
			CurrencyBalanceAccumulator total = byCurrency.computeIfAbsent(charge.charge().currency(),
				ignored -> new CurrencyBalanceAccumulator());
			total.amountBilled = total.amountBilled.add(charge.charge().amount());
			total.amountPaid = total.amountPaid.add(charge.paid());
			total.balance = total.balance.add(charge.balance());
		}
		List<CurrencyBalance> totals = byCurrency.entrySet().stream()
			.map(entry -> new CurrencyBalance(entry.getKey(), entry.getValue().amountBilled,
				entry.getValue().amountPaid, entry.getValue().balance)).toList();
		return new ChildSummary(student, establishment, year, schoolClass, totals);
	}

	@Transactional(readOnly = true)
	public List<ChargeBalance> findFees(String username, UUID studentId, UUID academicYearId) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		findEnrollment(student.establishmentId(), academicYearId, studentId);
		return tuitionApplicationService.findStudentChargesForGuardian(
			student.establishmentId(), studentId, academicYearId);
	}

	@Transactional(readOnly = true)
	public List<TuitionPayment> findPayments(String username, UUID studentId, UUID academicYearId,
		UUID chargeId, LocalDate from, LocalDate to) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		findEnrollment(student.establishmentId(), academicYearId, studentId);
		try {
			return tuitionApplicationService.findPaymentsForGuardian(
				student.establishmentId(), studentId, academicYearId, chargeId, from, to);
		} catch (TuitionNotFoundException exception) {
			throw new GuardianNotFoundException();
		} catch (TuitionValidationException exception) {
			throw new GuardianPortalValidationException(exception.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public AttendanceHistory findAttendance(String username, UUID studentId, UUID academicYearId,
		LocalDate from, LocalDate to) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		findEnrollment(student.establishmentId(), academicYearId, studentId);
		try {
			return attendanceApplicationService.findStudentHistoryForGuardian(
				student.establishmentId(), academicYearId, studentId, from, to);
		} catch (AttendanceNotFoundException exception) {
			throw new GuardianNotFoundException();
		} catch (AttendanceValidationException exception) {
			throw new GuardianPortalValidationException(exception.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public LearningContentPage findLearningContent(String username, UUID studentId, UUID academicYearId,
		LearningContentType type, int page, int size) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		var enrollment = findEnrollment(student.establishmentId(), academicYearId, studentId);
		return learningContentApplicationService.listPublishedForGuardian(student.establishmentId(),
			academicYearId, enrollment.schoolClassId(), type, page, size);
	}

	@Transactional(readOnly = true)
	public LearningContent getLearningContent(String username, UUID studentId, UUID academicYearId,
		UUID contentId) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		var enrollment = findEnrollment(student.establishmentId(), academicYearId, studentId);
		return learningContentApplicationService.getPublishedForGuardian(student.establishmentId(),
			academicYearId, enrollment.schoolClassId(), contentId);
	}

	@Transactional(readOnly = true)
	public List<LearningAttachment> findLearningAttachments(String username, UUID studentId,
		UUID academicYearId, UUID contentId) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		var enrollment = findEnrollment(student.establishmentId(), academicYearId, studentId);
		return learningContentApplicationService.listAttachmentsForGuardian(student.establishmentId(),
			academicYearId, enrollment.schoolClassId(), contentId);
	}

	@Transactional(readOnly = true)
	public LearningContentApplicationService.AttachmentDownload downloadLearningAttachment(String username,
		UUID studentId, UUID academicYearId, UUID contentId, UUID attachmentId) {
		Student student = findLinkedChild(username, studentId);
		findYear(student.establishmentId(), academicYearId);
		var enrollment = findEnrollment(student.establishmentId(), academicYearId, studentId);
		return learningContentApplicationService.downloadForGuardian(student.establishmentId(),
			academicYearId, enrollment.schoolClassId(), contentId, attachmentId);
	}

	private Student findLinkedChild(String username, UUID studentId) {
		Student student = guardianApplicationService.findLinkedChild(username, studentId);
		if (!licenseApplicationService.hasFeature(student.establishmentId(), LicenseFeature.PARENT_PORTAL,
			Instant.now())) {
			throw new GuardianAccessDeniedException();
		}
		return student;
	}

	private AcademicYear findYear(UUID establishmentId, UUID academicYearId) {
		try {
			return schoolCalendarApplicationService.findYearForGuardian(establishmentId, academicYearId);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new GuardianNotFoundException();
		}
	}

	private com.galactique.gestionecole.enrollments.domain.Enrollment findEnrollment(
		UUID establishmentId, UUID academicYearId, UUID studentId) {
		try {
			return enrollmentApplicationService.findEnrollmentForGuardian(establishmentId, academicYearId, studentId);
		} catch (EnrollmentNotFoundException exception) {
			throw new GuardianNotFoundException();
		}
	}

	private SchoolClass findClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		try {
			return schoolCatalogApplicationService.findClassForGuardian(establishmentId, academicYearId, schoolClassId);
		} catch (SchoolCatalogNotFoundException exception) {
			throw new GuardianNotFoundException();
		}
	}

	public record ChildSummary(Student student, Establishment establishment, AcademicYear academicYear,
		SchoolClass schoolClass, List<CurrencyBalance> balances) {
		public ChildSummary {
			balances = List.copyOf(balances);
		}
	}

	public record CurrencyBalance(String currency, java.math.BigDecimal amountBilled,
		java.math.BigDecimal amountPaid, java.math.BigDecimal balance) {
	}

	private static final class CurrencyBalanceAccumulator {
		private java.math.BigDecimal amountBilled = java.math.BigDecimal.ZERO;
		private java.math.BigDecimal amountPaid = java.math.BigDecimal.ZERO;
		private java.math.BigDecimal balance = java.math.BigDecimal.ZERO;
	}
}
