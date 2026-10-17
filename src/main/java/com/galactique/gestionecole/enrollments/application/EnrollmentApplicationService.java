package com.galactique.gestionecole.enrollments.application;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCalendarApplicationService;
import com.galactique.gestionecole.students.application.StudentAccessDeniedException;
import com.galactique.gestionecole.students.application.StudentApplicationService;
import com.galactique.gestionecole.students.application.StudentNotFoundException;
import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.enrollments.domain.EnrollmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;

@Service
public class EnrollmentApplicationService {
	private final EnrollmentRepository enrollmentRepository;
	private final StudentApplicationService studentApplicationService;
	private final SchoolCalendarApplicationService schoolCalendarApplicationService;
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;
	private final EnrollmentFeeChargeGateway enrollmentFeeChargeGateway;
	private final LicenseApplicationService licenseApplicationService;

	public EnrollmentApplicationService(EnrollmentRepository enrollmentRepository,
		StudentApplicationService studentApplicationService,
		SchoolCalendarApplicationService schoolCalendarApplicationService,
		SchoolCatalogApplicationService schoolCatalogApplicationService,
		EnrollmentFeeChargeGateway enrollmentFeeChargeGateway,
		LicenseApplicationService licenseApplicationService) {
		this.enrollmentRepository = enrollmentRepository;
		this.studentApplicationService = studentApplicationService;
		this.schoolCalendarApplicationService = schoolCalendarApplicationService;
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
		this.enrollmentFeeChargeGateway = enrollmentFeeChargeGateway;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Transactional(readOnly = true)
	public List<Enrollment> findAll(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		validateAcademicYear(establishmentId, academicYearId, actor);
		return enrollmentRepository.findAllByEstablishmentIdAndAcademicYearId(establishmentId, academicYearId);
	}

	@Transactional(readOnly = true)
	public List<Student> findStudentsByClass(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, EmployeeActor actor) {
		return findStudentsByClass(establishmentId, academicYearId, schoolClassId, null, actor);
	}

	@Transactional(readOnly = true)
	public List<Student> findStudentsByClass(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID classroomId, EmployeeActor actor) {
		validateAcademicYear(establishmentId, academicYearId, actor);
		try {
			schoolCatalogApplicationService.findClass(establishmentId, academicYearId, schoolClassId, actor);
			if (classroomId != null) {
				schoolCatalogApplicationService.findClassroom(establishmentId, academicYearId, schoolClassId, classroomId, actor);
			}
		} catch (SchoolCatalogNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (SchoolCatalogAccessDeniedException exception) {
			throw new EnrollmentAccessDeniedException();
		}
		List<Student> students = new java.util.ArrayList<>();
		for (Enrollment enrollment : enrollmentRepository.findAllByEstablishmentIdAndAcademicYearId(
			establishmentId, academicYearId)) {
			if (!enrollment.schoolClassId().equals(schoolClassId)) {
				continue;
			}
			if (classroomId != null && !enrollment.classroomId().equals(classroomId)) {
				continue;
			}
			try {
				students.add(studentApplicationService.findById(establishmentId, enrollment.studentId(), actor));
			} catch (StudentNotFoundException ignored) {
				continue;
			} catch (StudentAccessDeniedException exception) {
				throw new EnrollmentAccessDeniedException();
			}
		}
		return List.copyOf(students);
	}

	@Transactional(readOnly = true)
	public List<Enrollment> findEnrollmentsByClass(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, EmployeeActor actor) {
		validateAcademicYear(establishmentId, academicYearId, actor);
		try {
			schoolCatalogApplicationService.findClass(establishmentId, academicYearId, schoolClassId, actor);
		} catch (SchoolCatalogNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (SchoolCatalogAccessDeniedException exception) {
			throw new EnrollmentAccessDeniedException();
		}
		return enrollmentRepository.findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassId(
			establishmentId, academicYearId, schoolClassId);
	}

	@Transactional(readOnly = true)
	public Enrollment findEnrollmentForAttendanceHistory(UUID establishmentId, UUID academicYearId,
		UUID studentId, EmployeeActor actor) {
		try {
			schoolCalendarApplicationService.findYearForAttendanceHistory(establishmentId, academicYearId, actor);
			studentApplicationService.findIncludingArchived(establishmentId, studentId, actor);
			return enrollmentRepository.findByEstablishmentIdAndAcademicYearIdAndStudentId(
				establishmentId, academicYearId, studentId).orElseThrow(EnrollmentNotFoundException::new);
		} catch (AcademicCalendarNotFoundException | StudentNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (AcademicCalendarAccessDeniedException | StudentAccessDeniedException exception) {
			throw new EnrollmentAccessDeniedException();
		}
	}

	@Transactional(readOnly = true)
	public Enrollment findEnrollmentForGuardian(UUID establishmentId, UUID academicYearId, UUID studentId) {
		return enrollmentRepository.findByEstablishmentIdAndAcademicYearIdAndStudentId(
			establishmentId, academicYearId, studentId).orElseThrow(EnrollmentNotFoundException::new);
	}

	@Transactional
	public EnrollmentCreationResult create(UUID establishmentId, UUID academicYearId, CreateEnrollmentCommand command,
		EmployeeActor actor) {
		validateAcademicYear(establishmentId, academicYearId, actor);
		UUID classroomId;
		try {
			schoolCalendarApplicationService.lockYearForEnrollment(establishmentId, academicYearId, actor);
			studentApplicationService.findById(establishmentId, command.studentId(), actor);
			classroomId = schoolCatalogApplicationService.resolveEnrollmentClassroom(establishmentId, academicYearId,
				command.schoolClassId(), command.classroomId(), actor).id();
		} catch (StudentNotFoundException | AcademicCalendarNotFoundException | SchoolCatalogNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (StudentAccessDeniedException | AcademicCalendarAccessDeniedException
			| SchoolCatalogAccessDeniedException exception) {
			throw new EnrollmentAccessDeniedException();
		}
		if (enrollmentRepository.existsByStudentIdAndAcademicYearId(command.studentId(), academicYearId)) {
			throw new EnrollmentConflictException();
		}
		try {
			Enrollment enrollment = enrollmentRepository.save(establishmentId, command.studentId(), academicYearId,
				command.schoolClassId(), classroomId, command.acceptAdvance(), actor.username());
			var payments = enrollmentFeeChargeGateway.applyFeesAndRecordPayments(enrollment, command.payments(), actor);
			return new EnrollmentCreationResult(enrollment, payments);
		} catch (DataIntegrityViolationException exception) {
			throw new EnrollmentConflictException();
		}
	}

	@Transactional
	public Enrollment changeClassroom(UUID establishmentId, UUID academicYearId, UUID enrollmentId,
		UUID classroomId, EmployeeActor actor) {
		validateAcademicYear(establishmentId, academicYearId, actor);
		Enrollment enrollment = enrollmentRepository.findById(establishmentId, academicYearId, enrollmentId)
			.orElseThrow(EnrollmentNotFoundException::new);
		try {
			studentApplicationService.findById(establishmentId, enrollment.studentId(), actor);
			var classroom = schoolCatalogApplicationService.resolveEnrollmentClassroom(establishmentId, academicYearId,
				enrollment.schoolClassId(), classroomId, actor);
			return enrollmentRepository.updateClassroom(enrollment, classroom.id());
		} catch (StudentNotFoundException | SchoolCatalogNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (StudentAccessDeniedException | SchoolCatalogAccessDeniedException exception) {
			throw new EnrollmentAccessDeniedException();
		}
	}

	private void validateAcademicYear(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		if (!licenseApplicationService.hasFeature(establishmentId, LicenseFeature.STUDENT_RECORDS,
			java.time.Instant.now())) {
			throw new EnrollmentAccessDeniedException();
		}
		try {
			schoolCalendarApplicationService.findYear(establishmentId, academicYearId, actor);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new EnrollmentNotFoundException();
		} catch (AcademicCalendarAccessDeniedException exception) {
			throw new EnrollmentAccessDeniedException();
		}
	}
}
