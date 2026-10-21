package com.galactique.gestionecole.attendance.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;
import com.galactique.gestionecole.attendance.domain.AttendanceRepository;
import com.galactique.gestionecole.attendance.domain.AttendanceStatus;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.enrollments.application.EnrollmentAccessDeniedException;
import com.galactique.gestionecole.enrollments.application.EnrollmentApplicationService;
import com.galactique.gestionecole.enrollments.application.EnrollmentNotFoundException;
import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.AcademicCalendarNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCalendarApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogAccessDeniedException;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogNotFoundException;
import com.galactique.gestionecole.schoolclasses.application.SchoolClassOperationalityService;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;
import com.galactique.gestionecole.students.application.StudentAccessDeniedException;
import com.galactique.gestionecole.students.application.StudentApplicationService;
import com.galactique.gestionecole.students.application.StudentNotFoundException;
import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;

@Service
public class AttendanceApplicationService {
	private static final Logger logger = LoggerFactory.getLogger(AttendanceApplicationService.class);
	private static final long MAX_HISTORY_DAYS = 366;

	private final AttendanceRepository attendanceRepository;
	private final SchoolCalendarApplicationService schoolCalendarApplicationService;
	private final SchoolCatalogApplicationService schoolCatalogApplicationService;
	private final EnrollmentApplicationService enrollmentApplicationService;
	private final StudentApplicationService studentApplicationService;
	private final LicenseApplicationService licenseApplicationService;
	private final SchoolClassOperationalityService schoolClassOperationalityService;

	public AttendanceApplicationService(AttendanceRepository attendanceRepository,
		SchoolCalendarApplicationService schoolCalendarApplicationService,
		SchoolCatalogApplicationService schoolCatalogApplicationService,
		EnrollmentApplicationService enrollmentApplicationService,
		StudentApplicationService studentApplicationService,
		LicenseApplicationService licenseApplicationService,
		SchoolClassOperationalityService schoolClassOperationalityService) {
		this.attendanceRepository = attendanceRepository;
		this.schoolCalendarApplicationService = schoolCalendarApplicationService;
		this.schoolCatalogApplicationService = schoolCatalogApplicationService;
		this.enrollmentApplicationService = enrollmentApplicationService;
		this.studentApplicationService = studentApplicationService;
		this.licenseApplicationService = licenseApplicationService;
		this.schoolClassOperationalityService = schoolClassOperationalityService;
	}

	@Transactional(readOnly = true)
	public AttendanceSheet findClassAttendance(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID classroomId, LocalDate date, EmployeeActor actor) {
		AcademicYear year = findActiveYear(establishmentId, academicYearId, actor);
		validateDate(date, year);
		findActiveClass(establishmentId, academicYearId, schoolClassId, actor);
		schoolClassOperationalityService.requireActiveClassroom(establishmentId, academicYearId, schoolClassId);
		if (classroomId != null) { requireClassroom(establishmentId, academicYearId, schoolClassId, classroomId, actor, false); }
		List<Enrollment> enrollments = findClassEnrollments(establishmentId, academicYearId, schoolClassId, actor).stream()
			.filter(enrollment -> classroomId == null || classroomId.equals(enrollment.classroomId())).toList();
		List<Student> students = findActiveStudents(establishmentId, enrollments, actor);
		return buildSheet(establishmentId, academicYearId, schoolClassId, classroomId, date, enrollments, students,
			attendanceRepository.findByEnrollmentIdsAndDate(
				enrollments.stream().map(Enrollment::id).toList(), date));
	}

	@Transactional
	public AttendanceSheet recordClassAttendance(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, UUID classroomId, LocalDate date, List<AttendanceEntryCommand> commands, EmployeeActor actor) {
		AcademicYear year = findActiveYear(establishmentId, academicYearId, actor);
		validateDate(date, year);
		findActiveClass(establishmentId, academicYearId, schoolClassId, actor);
		schoolClassOperationalityService.requireActiveClassroom(establishmentId, academicYearId, schoolClassId);
		if (classroomId == null) { throw new AttendanceValidationException("classroomId is required for every attendance batch"); }
		// The same class lock is used for enrollment transfers and classroom archival.
		requireClassroom(establishmentId, academicYearId, schoolClassId, classroomId, actor, true);
		List<Enrollment> enrollments = findClassEnrollments(establishmentId, academicYearId, schoolClassId, actor).stream()
			.filter(enrollment -> classroomId.equals(enrollment.classroomId())).toList();
		List<Student> students = findActiveStudents(establishmentId, enrollments, actor);
		Map<UUID, Enrollment> enrollmentByStudent = mapEnrollmentsByStudent(enrollments);
		Map<UUID, Student> studentById = mapStudentsById(students);
		Map<UUID, AttendanceEntryCommand> uniqueCommands = validateCommands(
			commands, studentById, enrollmentByStudent);

		List<UUID> enrollmentIds = uniqueCommands.keySet().stream()
			.map(enrollmentByStudent::get).map(Enrollment::id).toList();
		Map<UUID, AttendanceRecord> currentByEnrollment = lockCurrentRecords(enrollmentIds, date);
		validateExpectedVersions(uniqueCommands, enrollmentByStudent, currentByEnrollment);
		applyCommands(uniqueCommands, enrollmentByStudent, currentByEnrollment, date, actor.username());

		return buildSheet(establishmentId, academicYearId, schoolClassId, classroomId, date, enrollments, students,
			attendanceRepository.findByEnrollmentIdsAndDate(enrollments.stream().map(Enrollment::id).toList(), date));
	}

	@Transactional(readOnly = true)
	public AttendanceHistory findStudentHistory(UUID establishmentId, UUID academicYearId, UUID studentId,
		LocalDate from, LocalDate to, EmployeeActor actor) {
		requireAttendanceLicense(establishmentId);
		AcademicYear year;
		try {
			year = schoolCalendarApplicationService.findYearForAttendanceHistory(establishmentId, academicYearId, actor);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (AcademicCalendarAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
		validateDateRange(from, to, year);
		Enrollment enrollment;
		try {
			enrollment = enrollmentApplicationService.findEnrollmentForAttendanceHistory(
				establishmentId, academicYearId, studentId, actor);
		} catch (EnrollmentNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (EnrollmentAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
		return new AttendanceHistory(establishmentId, academicYearId, studentId, from, to,
			attendanceRepository.findByEnrollmentIdAndDateRange(enrollment.id(), from, to));
	}

	@Transactional(readOnly = true)
	public AttendanceHistory findStudentHistoryForGuardian(UUID establishmentId, UUID academicYearId,
		UUID studentId, LocalDate from, LocalDate to) {
		requireAttendanceLicense(establishmentId);
		AcademicYear year;
		try {
			year = schoolCalendarApplicationService.findYearForGuardian(establishmentId, academicYearId);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new AttendanceNotFoundException();
		}
		validateDateRange(from, to, year);
		Enrollment enrollment;
		try {
			enrollment = enrollmentApplicationService.findEnrollmentForGuardian(establishmentId, academicYearId, studentId);
		} catch (EnrollmentNotFoundException exception) {
			throw new AttendanceNotFoundException();
		}
		return new AttendanceHistory(establishmentId, academicYearId, studentId, from, to,
			attendanceRepository.findByEnrollmentIdAndDateRange(enrollment.id(), from, to));
	}

	private AttendanceSheet buildSheet(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID classroomId, LocalDate date, List<Enrollment> enrollments, List<Student> students, List<AttendanceRecord> records) {
		Map<UUID, Enrollment> enrollmentByStudent = new HashMap<>();
		for (Enrollment enrollment : enrollments) {
			enrollmentByStudent.put(enrollment.studentId(), enrollment);
		}
		Map<UUID, AttendanceRecord> recordByEnrollment = new HashMap<>();
		for (AttendanceRecord attendanceRecord : records) {
			recordByEnrollment.put(attendanceRecord.enrollmentId(), attendanceRecord);
		}
		List<AttendanceSheetEntry> entries = new ArrayList<>();
		int present = 0;
		int absent = 0;
		for (Student student : students) {
			Enrollment enrollment = enrollmentByStudent.get(student.id());
			AttendanceRecord attendanceRecord = enrollment == null ? null : recordByEnrollment.get(enrollment.id());
			if (attendanceRecord == null) {
				entries.add(new AttendanceSheetEntry(student, null));
			} else {
				if (attendanceRecord.status() == AttendanceStatus.PRESENT) {
					present++;
				} else {
					absent++;
				}
				entries.add(new AttendanceSheetEntry(student, attendanceRecord));
			}
		}
		return new AttendanceSheet(establishmentId, academicYearId, schoolClassId, classroomId, date,
			present, absent, entries.size() - present - absent, entries);
	}

	private AttendanceEntryCommand validateCommand(AttendanceEntryCommand command,
		Map<UUID, Student> students, Map<UUID, Enrollment> enrollments) {
		if (command == null || command.studentId() == null || command.status() == null) {
			throw new AttendanceValidationException("Each entry requires a student and a status");
		}
		if (!students.containsKey(command.studentId()) || !enrollments.containsKey(command.studentId())) {
			throw new AttendanceValidationException("Every student must be actively enrolled in the requested classroom");
		}
		String comment = command.comment() == null || command.comment().isBlank() ? null : command.comment().strip();
		String parentComment = command.parentComment() == null || command.parentComment().isBlank()
			? null : command.parentComment().strip();
		if (comment != null && comment.length() > 500) {
			throw new AttendanceValidationException("Comment must not exceed 500 characters");
		}
		if (parentComment != null && parentComment.length() > 500) {
			throw new AttendanceValidationException("Parent comment must not exceed 500 characters");
		}
		if (command.absenceJustified() && command.status() != AttendanceStatus.ABSENT) {
			throw new AttendanceValidationException("Only an absence can be justified");
		}
		return new AttendanceEntryCommand(command.studentId(), command.status(), command.absenceJustified(),
			comment, parentComment, command.expectedUpdatedAt());
	}

	private Map<UUID, AttendanceEntryCommand> validateCommands(List<AttendanceEntryCommand> commands,
		Map<UUID, Student> students, Map<UUID, Enrollment> enrollments) {
		Map<UUID, AttendanceEntryCommand> uniqueCommands = new LinkedHashMap<>();
		for (AttendanceEntryCommand command : commands) {
			AttendanceEntryCommand normalized = validateCommand(command, students, enrollments);
			AttendanceEntryCommand previous = uniqueCommands.putIfAbsent(normalized.studentId(), normalized);
			if (previous != null && !previous.equals(normalized)) {
				throw new AttendanceValidationException("A student has contradictory entries in the same request");
			}
		}
		return uniqueCommands;
	}

	private Map<UUID, Enrollment> mapEnrollmentsByStudent(List<Enrollment> enrollments) {
		Map<UUID, Enrollment> result = new HashMap<>();
		for (Enrollment enrollment : enrollments) {
			result.put(enrollment.studentId(), enrollment);
		}
		return result;
	}

	private Map<UUID, Student> mapStudentsById(List<Student> students) {
		Map<UUID, Student> result = new HashMap<>();
		for (Student student : students) {
			result.put(student.id(), student);
		}
		return result;
	}

	private Map<UUID, AttendanceRecord> lockCurrentRecords(List<UUID> enrollmentIds, LocalDate date) {
		Map<UUID, AttendanceRecord> result = new HashMap<>();
		for (AttendanceRecord attendanceRecord : attendanceRepository.findByEnrollmentIdsAndDateForUpdate(
			enrollmentIds, date)) {
			result.put(attendanceRecord.enrollmentId(), attendanceRecord);
		}
		return result;
	}

	private void validateExpectedVersions(Map<UUID, AttendanceEntryCommand> commands,
		Map<UUID, Enrollment> enrollments, Map<UUID, AttendanceRecord> currentByEnrollment) {
		for (AttendanceEntryCommand command : commands.values()) {
			Enrollment enrollment = enrollments.get(command.studentId());
			AttendanceRecord current = currentByEnrollment.get(enrollment.id());
			if ((current == null && command.expectedUpdatedAt() != null)
				|| (current != null && hasChanges(current, command)
					&& !current.updatedAt().equals(command.expectedUpdatedAt()))) {
				logger.warn("Attendance version conflict: enrollmentId={}, expectedUpdatedAt={}, actualUpdatedAt={}, reason={}",
					enrollment.id(), command.expectedUpdatedAt(), current == null ? null : current.updatedAt(),
					current == null ? "record_missing" : command.expectedUpdatedAt() == null ? "version_missing" : "version_stale");
				throw new AttendanceConflictException();
			}
		}
	}

	private void applyCommands(Map<UUID, AttendanceEntryCommand> commands,
		Map<UUID, Enrollment> enrollments, Map<UUID, AttendanceRecord> currentByEnrollment,
		LocalDate date, String username) {
		try {
			for (AttendanceEntryCommand command : commands.values()) {
				Enrollment enrollment = enrollments.get(command.studentId());
				AttendanceRecord current = currentByEnrollment.get(enrollment.id());
				if (current == null) {
					attendanceRepository.save(enrollment.id(), date, command.status(), command.absenceJustified(),
						command.comment(), command.parentComment(), username);
				} else if (hasChanges(current, command)) {
					attendanceRepository.update(current, command.status(), command.absenceJustified(),
						command.comment(), command.parentComment(), username);
				}
			}
		} catch (DataIntegrityViolationException exception) {
			throw new AttendanceConflictException(exception);
		}
	}

	private boolean hasChanges(AttendanceRecord current, AttendanceEntryCommand command) {
		return current.status() != command.status()
			|| current.absenceJustified() != command.absenceJustified()
			|| !Objects.equals(current.comment(), command.comment())
			|| !Objects.equals(current.parentComment(), command.parentComment());
	}

	private AcademicYear findActiveYear(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		requireAttendanceLicense(establishmentId);
		try {
			return schoolCalendarApplicationService.findYear(establishmentId, academicYearId, actor);
		} catch (AcademicCalendarNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (AcademicCalendarAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
	}

	private void requireAttendanceLicense(UUID establishmentId) {
		if (!licenseApplicationService.canAccessFeature(establishmentId, LicenseFeature.ATTENDANCE,
			java.time.Instant.now())) {
			throw new AttendanceAccessDeniedException();
		}
	}

	private SchoolClass findActiveClass(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, EmployeeActor actor) {
		try {
			return schoolCatalogApplicationService.findClass(establishmentId, academicYearId, schoolClassId, actor);
		} catch (SchoolCatalogNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (SchoolCatalogAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
	}

	private List<Enrollment> findClassEnrollments(UUID establishmentId, UUID academicYearId,
		UUID schoolClassId, EmployeeActor actor) {
		try {
			return enrollmentApplicationService.findEnrollmentsByClass(
				establishmentId, academicYearId, schoolClassId, actor);
		} catch (EnrollmentNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (EnrollmentAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
	}

	private void requireClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID classroomId, EmployeeActor actor, boolean lock) {
		try {
			if (lock) {
				schoolCatalogApplicationService.resolveEnrollmentClassroom(establishmentId, academicYearId, schoolClassId, classroomId, actor);
			} else {
				schoolCatalogApplicationService.findClassroom(establishmentId, academicYearId, schoolClassId, classroomId, actor);
			}
		} catch (SchoolCatalogNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (SchoolCatalogAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
	}

	private List<Student> findActiveStudents(UUID establishmentId, List<Enrollment> enrollments, EmployeeActor actor) {
		try {
			return studentApplicationService.findActiveByIds(establishmentId,
				enrollments.stream().map(Enrollment::studentId).distinct().toList(), actor);
		} catch (StudentNotFoundException exception) {
			throw new AttendanceNotFoundException();
		} catch (StudentAccessDeniedException exception) {
			throw new AttendanceAccessDeniedException();
		}
	}

	private void validateDate(LocalDate date, AcademicYear year) {
		if (date == null || date.isBefore(year.startsOn()) || date.isAfter(year.endsOn())) {
			throw new AttendanceValidationException("Attendance date must be within the academic year");
		}
	}

	private void validateDateRange(LocalDate from, LocalDate to, AcademicYear year) {
		if (from == null || to == null || to.isBefore(from)
			|| from.isBefore(year.startsOn()) || to.isAfter(year.endsOn())
			|| ChronoUnit.DAYS.between(from, to) >= MAX_HISTORY_DAYS) {
			throw new AttendanceValidationException("Attendance history range must be within the academic year and not exceed 366 days");
		}
	}
}
