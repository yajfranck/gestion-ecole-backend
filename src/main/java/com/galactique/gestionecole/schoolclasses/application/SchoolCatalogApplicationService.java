package com.galactique.gestionecole.schoolclasses.application;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassSubjectRepository;
import com.galactique.gestionecole.schoolclasses.domain.Classroom;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomSubjectTeacher;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomSubjectTeacherRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassSubject;
import com.galactique.gestionecole.schoolclasses.domain.Subject;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;

@Service
public class SchoolCatalogApplicationService {
	private final ClassroomSubjectTeacherRepository subjectTeachers;
	private final EmployeeRepository employees;
	private final SubjectRepository subjectRepository;
	private final SchoolClassRepository schoolClassRepository;
	private final ClassSubjectRepository classSubjectRepository;
	private final AcademicYearRepository academicYearRepository;
	private final EmployeeApplicationService employeeApplicationService;
	private final LicenseApplicationService licenseApplicationService;
	private final ClassroomRepository classroomRepository;
	private final SchoolClassOperationalityService schoolClassOperationalityService;

	public SchoolCatalogApplicationService(
		SubjectRepository subjectRepository,
		SchoolClassRepository schoolClassRepository,
		ClassSubjectRepository classSubjectRepository,
		AcademicYearRepository academicYearRepository,
		EmployeeApplicationService employeeApplicationService,
		LicenseApplicationService licenseApplicationService,
		ClassroomRepository classroomRepository, ClassroomSubjectTeacherRepository subjectTeachers,
		EmployeeRepository employees, SchoolClassOperationalityService schoolClassOperationalityService) {
		this.subjectTeachers = subjectTeachers;
		this.employees = employees;
		this.subjectRepository = subjectRepository;
		this.schoolClassRepository = schoolClassRepository;
		this.classSubjectRepository = classSubjectRepository;
		this.academicYearRepository = academicYearRepository;
		this.employeeApplicationService = employeeApplicationService;
		this.licenseApplicationService = licenseApplicationService;
		this.classroomRepository = classroomRepository;
		this.schoolClassOperationalityService = schoolClassOperationalityService;
	}

	@Transactional(readOnly = true)
	public List<Subject> findSubjects(UUID establishmentId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return subjectRepository.findAllByEstablishmentId(establishmentId).stream()
			.sorted(java.util.Comparator.comparing(Subject::name)).toList();
	}

	@Transactional(readOnly = true)
	public Subject findSubject(UUID establishmentId, UUID subjectId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return subjectRepository.findByIdAndEstablishmentId(subjectId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
	}

	@Transactional
	public Subject createSubject(UUID establishmentId, CreateSubjectCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		String code = command.code() == null ? null : normalizeCode(command.code());
		if (code != null && subjectRepository.existsByEstablishmentIdAndCode(establishmentId, code)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return subjectRepository.save(establishmentId, code, command.name());
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public List<Subject> createSubjects(UUID establishmentId, List<CreateSubjectCommand> commands, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		if (commands == null || commands.isEmpty() || commands.size() > 100 || commands.stream().anyMatch(java.util.Objects::isNull)) {
			throw new IllegalArgumentException("Provide between 1 and 100 subjects");
		}
		return commands.stream().map(command -> createSubject(establishmentId, command, actor)).toList();
	}

	@Transactional
	public Subject updateSubject(UUID establishmentId, UUID subjectId,
		UpdateSubjectCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		Subject current = subjectRepository.findByIdAndEstablishmentId(subjectId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		String code = command.code() == null ? current.code() : normalizeCode(command.code());
		String name = command.name() == null ? current.name() : command.name();
		if (code != null && subjectRepository.existsByEstablishmentIdAndCodeAndIdNot(establishmentId, code, subjectId)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return subjectRepository.update(subjectId, establishmentId, code, name);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public void archiveSubject(UUID establishmentId, UUID subjectId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		subjectRepository.findByIdAndEstablishmentId(subjectId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		if (subjectRepository.hasActiveClassAssignments(establishmentId, subjectId)) {
			throw new SchoolCatalogConflictException();
		}
		subjectRepository.archive(subjectId, establishmentId);
	}

	@Transactional(readOnly = true)
	public List<SchoolClass> findClasses(UUID establishmentId, UUID academicYearId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		return schoolClassRepository.findAllByEstablishmentIdAndAcademicYearId(establishmentId, academicYearId);
	}

	@Transactional(readOnly = true)
	public SchoolClass findClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		return findClass(establishmentId, academicYearId, schoolClassId);
	}

	@Transactional(readOnly = true)
	public SchoolClass findClassForGuardian(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		return schoolClassRepository.findIncludingArchivedByIdAndEstablishmentIdAndAcademicYearId(
			schoolClassId, establishmentId, academicYearId).orElseThrow(SchoolCatalogNotFoundException::new);
	}

	@Transactional
	public SchoolClass createClass(UUID establishmentId, UUID academicYearId,
		CreateSchoolClassCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		return createClassInScope(establishmentId, academicYearId, command);
	}

	@Transactional
	public List<SchoolClass> createClasses(UUID establishmentId, UUID academicYearId,
		List<CreateSchoolClassCommand> commands, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		if (commands == null || commands.isEmpty() || commands.size() > 100) {
			throw new IllegalArgumentException("Provide between 1 and 100 classes");
		}
		Set<String> requestedCodes = new HashSet<>();
		for (int index = 0; index < commands.size(); index++) {
			try {
				validateClassCommand(commands.get(index));
				String code = normalizeCode(commands.get(index).code());
				if (!requestedCodes.add(code)) {
					throw new SchoolClassBulkValidationException(index, "class code is duplicated in this request");
				}
				if (schoolClassRepository.existsByAcademicYearIdAndCode(academicYearId, code)) {
					throw new SchoolClassBulkConflictException(index, "class code already exists in this academic year");
				}
			} catch (IllegalArgumentException exception) {
				throw new SchoolClassBulkValidationException(index, exception.getMessage());
			}
		}
		List<SchoolClass> created = new java.util.ArrayList<>(commands.size());
		for (int index = 0; index < commands.size(); index++) {
			try {
				created.add(createClassInScope(establishmentId, academicYearId, commands.get(index)));
			} catch (SchoolCatalogConflictException exception) {
				throw new SchoolClassBulkConflictException(index, "class code already exists in this academic year");
			} catch (IllegalArgumentException exception) {
				throw new SchoolClassBulkValidationException(index, exception.getMessage());
			}
		}
		return List.copyOf(created);
	}

	private SchoolClass createClassInScope(UUID establishmentId, UUID academicYearId,
		CreateSchoolClassCommand command) {
		validateClassCommand(command);
		String code = normalizeCode(command.code());
		if (schoolClassRepository.existsByAcademicYearIdAndCode(academicYearId, code)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return schoolClassRepository.save(establishmentId, academicYearId, code, command.name(),
				SchoolClass.normalizeSeries(command.series()));
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	private void validateClassCommand(CreateSchoolClassCommand command) {
		if (command == null || command.code() == null || command.code().isBlank() || command.code().strip().length() > 40) {
			throw new IllegalArgumentException("code must contain between 1 and 40 non-blank characters");
		}
		if (command.name() == null || command.name().isBlank() || command.name().strip().length() > 100) {
			throw new IllegalArgumentException("name must contain between 1 and 100 non-blank characters");
		}
		SchoolClass.normalizeSeries(command.series());
	}

	@Transactional
	public SchoolClass updateClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UpdateSchoolClassCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		SchoolClass current = findClass(establishmentId, academicYearId, schoolClassId);
		String code = command.code() == null ? current.code() : normalizeCode(command.code());
		String name = command.name() == null ? current.name() : command.name();
		String series = command.series() == null ? current.series() : SchoolClass.normalizeSeries(command.series());
		if (schoolClassRepository.existsByAcademicYearIdAndCodeAndIdNot(academicYearId, code, schoolClassId)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return schoolClassRepository.update(schoolClassId, establishmentId, academicYearId, code, name, series);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public void archiveClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		lockClass(establishmentId, academicYearId, schoolClassId);
		schoolClassRepository.archive(schoolClassId, establishmentId, academicYearId);
	}

	@Transactional(readOnly = true)
	public List<Classroom> findClassrooms(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		EmployeeActor actor) {
		findClass(establishmentId, academicYearId, schoolClassId, actor);
		return classroomRepository.findAll(establishmentId, academicYearId, schoolClassId);
	}

	@Transactional(readOnly = true)
	public Classroom findClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
		EmployeeActor actor) {
		findClass(establishmentId, academicYearId, schoolClassId, actor);
		return requireClassroom(establishmentId, academicYearId, schoolClassId, classroomId);
	}

	@Transactional
	public Classroom createClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		String code, String name, int maxStudents, EmployeeActor actor) {
		findClass(establishmentId, academicYearId, schoolClassId, actor);
		lockClass(establishmentId, academicYearId, schoolClassId);
		try {
			return classroomRepository.save(establishmentId, academicYearId, schoolClassId, normalizeCode(code), name,
				maxStudents);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public Classroom updateClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
		String code, String name, Integer maxStudents, EmployeeActor actor) {
		findClass(establishmentId, academicYearId, schoolClassId, actor);
		lockClass(establishmentId, academicYearId, schoolClassId);
		Classroom current = requireClassroom(establishmentId, academicYearId, schoolClassId, classroomId);
		int updatedMaxStudents = maxStudents == null ? current.maxStudents() : maxStudents;
		if (updatedMaxStudents < classroomRepository.countEnrollments(classroomId)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return classroomRepository.update(current, code == null ? current.code() : normalizeCode(code),
				name == null ? current.name() : name, updatedMaxStudents);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public void archiveClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
		EmployeeActor actor) {
		findClass(establishmentId, academicYearId, schoolClassId, actor);
		lockClass(establishmentId, academicYearId, schoolClassId);
		Classroom current = requireClassroom(establishmentId, academicYearId, schoolClassId, classroomId);
		if (classroomRepository.hasEnrollments(classroomId) || classroomRepository.hasTimetableEntries(classroomId)) {
			throw new SchoolCatalogConflictException();
		}
		classroomRepository.archive(current);
	}

	/** Locks the class until the enclosing enrollment transaction commits. */
	@Transactional
	public Classroom resolveEnrollmentClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID classroomId, EmployeeActor actor) {
		findClass(establishmentId, academicYearId, schoolClassId, actor);
		lockClass(establishmentId, academicYearId, schoolClassId);
		schoolClassOperationalityService.requireActiveClassroom(establishmentId, academicYearId, schoolClassId);
		if (classroomId != null) {
			return requireClassroom(establishmentId, academicYearId, schoolClassId, classroomId);
		}
		List<Classroom> classrooms = classroomRepository.findAll(establishmentId, academicYearId, schoolClassId);
		if (classrooms.size() != 1) {
			throw new IllegalArgumentException("classroomId is required unless the class has exactly one active classroom");
		}
		return classrooms.getFirst();
	}

	private Classroom requireClassroom(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId) {
		return classroomRepository.findById(establishmentId, academicYearId, schoolClassId, classroomId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
	}

	private SchoolClass lockClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		return schoolClassRepository.findForUpdate(schoolClassId, establishmentId, academicYearId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public List<SchoolClassSubject> findClassSubjects(
		UUID establishmentId, UUID academicYearId, UUID schoolClassId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		findClass(establishmentId, academicYearId, schoolClassId);
		return classSubjectRepository.findAllByEstablishmentIdAndClassId(establishmentId, schoolClassId);
	}

	@Transactional
	public SchoolClassSubject assignSubject(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID subjectId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		findClass(establishmentId, academicYearId, schoolClassId);
		subjectRepository.findByIdAndEstablishmentId(subjectId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		var existing = classSubjectRepository.findByEstablishmentIdAndClassIdAndSubjectId(
			establishmentId, schoolClassId, subjectId);
		if (existing.isPresent()) {
			if (existing.get().enabled()) {
				throw new SchoolCatalogConflictException();
			}
			return classSubjectRepository.enable(existing.get().id(), establishmentId);
		}
		try {
			return classSubjectRepository.save(establishmentId, schoolClassId, subjectId);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public SchoolClassSubject assignSubject(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID subjectId, List<ClassroomSubjectTeacher> teachers,
		EmployeeActor actor) {
		if (teachers == null) { return assignSubject(establishmentId, academicYearId, schoolClassId, subjectId, actor); }
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		schoolClassRepository.findForUpdate(schoolClassId, establishmentId, academicYearId).orElseThrow(SchoolCatalogNotFoundException::new);
		if (!teachers.isEmpty()) {
			schoolClassOperationalityService.requireActiveClassroom(establishmentId, academicYearId, schoolClassId);
		}
		subjectRepository.findByIdAndEstablishmentId(subjectId, establishmentId).orElseThrow(SchoolCatalogNotFoundException::new);
		var roomIds = new java.util.HashSet<UUID>();
		for (var teacher : teachers) {
			if (teacher == null || teacher.classroomId() == null || teacher.employeeId() == null
				|| !roomIds.add(teacher.classroomId())) { throw new IllegalArgumentException("One teacher per classroom is allowed"); }
			requireClassroom(establishmentId, academicYearId, schoolClassId, teacher.classroomId());
			employees.findById(teacher.employeeId()).filter(employee -> employee.enabled()
				&& establishmentId.equals(employee.establishmentId())).orElseThrow(SchoolCatalogNotFoundException::new);
		}
		var current = classSubjectRepository.findByEstablishmentIdAndClassIdAndSubjectId(establishmentId, schoolClassId, subjectId);
		SchoolClassSubject result = current.isPresent() && current.get().enabled() ? current.get()
			: assignSubject(establishmentId, academicYearId, schoolClassId, subjectId, actor);
		subjectTeachers.replace(establishmentId, academicYearId, schoolClassId, subjectId, teachers);
		return result;
	}

	@Transactional(readOnly = true)
	public List<ClassroomSubjectTeacher> findSubjectTeachers(SchoolClassSubject assignment) {
		return subjectTeachers.findAllByClass(assignment.establishmentId(), assignment.schoolClassId())
			.getOrDefault(assignment.subjectId(), List.of());
	}

	@Transactional
	public void unassignSubject(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UUID subjectId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		findClass(establishmentId, academicYearId, schoolClassId);
		var assignment = classSubjectRepository.findByEstablishmentIdAndClassIdAndSubjectId(
			establishmentId, schoolClassId, subjectId).filter(SchoolClassSubject::enabled)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		classSubjectRepository.disable(assignment.id(), establishmentId);
	}

	private SchoolClass findClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId) {
		return schoolClassRepository.findByIdAndEstablishmentIdAndAcademicYearId(
			schoolClassId, establishmentId, academicYearId).orElseThrow(SchoolCatalogNotFoundException::new);
	}

	private AcademicYear findAcademicYear(UUID establishmentId, UUID academicYearId) {
		return academicYearRepository.findByIdAndEstablishmentId(academicYearId, establishmentId)
			.filter(AcademicYear::enabled)
			.orElseThrow(SchoolCatalogNotFoundException::new);
	}

	private void requireEstablishment(UUID establishmentId, EmployeeActor actor) {
		if (!licenseApplicationService.canAccessFeature(establishmentId, LicenseFeature.SCHOOL_MANAGEMENT,
			java.time.Instant.now())) {
			throw new SchoolCatalogAccessDeniedException();
		}
		try {
			boolean accessible = employeeApplicationService.findEstablishments(actor).stream()
				.anyMatch(establishment -> establishment.enabled() && establishment.id().equals(establishmentId));
			if (!accessible) {
				throw new SchoolCatalogNotFoundException();
			}
		} catch (EmployeeAccessDeniedException exception) {
			throw new SchoolCatalogAccessDeniedException();
		}
	}

	private String normalizeCode(String code) {
		return code.strip().toUpperCase(Locale.ROOT);
	}
}
