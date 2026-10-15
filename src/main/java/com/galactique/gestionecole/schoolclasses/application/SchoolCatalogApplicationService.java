package com.galactique.gestionecole.schoolclasses.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassSubjectRepository;
import com.galactique.gestionecole.schoolclasses.domain.GradeLevel;
import com.galactique.gestionecole.schoolclasses.domain.GradeLevelRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassSubject;
import com.galactique.gestionecole.schoolclasses.domain.Subject;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;

@Service
public class SchoolCatalogApplicationService {
	private final GradeLevelRepository gradeLevelRepository;
	private final SubjectRepository subjectRepository;
	private final SchoolClassRepository schoolClassRepository;
	private final ClassSubjectRepository classSubjectRepository;
	private final AcademicYearRepository academicYearRepository;
	private final EmployeeApplicationService employeeApplicationService;

	public SchoolCatalogApplicationService(
		GradeLevelRepository gradeLevelRepository,
		SubjectRepository subjectRepository,
		SchoolClassRepository schoolClassRepository,
		ClassSubjectRepository classSubjectRepository,
		AcademicYearRepository academicYearRepository,
		EmployeeApplicationService employeeApplicationService) {
		this.gradeLevelRepository = gradeLevelRepository;
		this.subjectRepository = subjectRepository;
		this.schoolClassRepository = schoolClassRepository;
		this.classSubjectRepository = classSubjectRepository;
		this.academicYearRepository = academicYearRepository;
		this.employeeApplicationService = employeeApplicationService;
	}

	@Transactional(readOnly = true)
	public List<GradeLevel> findGradeLevels(UUID establishmentId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return gradeLevelRepository.findAllByEstablishmentId(establishmentId);
	}

	@Transactional(readOnly = true)
	public GradeLevel findGradeLevel(UUID establishmentId, UUID gradeLevelId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return gradeLevelRepository.findByIdAndEstablishmentId(gradeLevelId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
	}

	@Transactional
	public GradeLevel createGradeLevel(UUID establishmentId, CreateGradeLevelCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		String code = normalizeCode(command.code());
		if (gradeLevelRepository.existsByEstablishmentIdAndCode(establishmentId, code)
			|| gradeLevelRepository.existsByEstablishmentIdAndName(establishmentId, command.name().strip())) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return gradeLevelRepository.save(establishmentId, code, command.name(), command.displayOrder());
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public GradeLevel updateGradeLevel(UUID establishmentId, UUID gradeLevelId,
		UpdateGradeLevelCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		GradeLevel current = gradeLevelRepository.findByIdAndEstablishmentId(gradeLevelId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		String code = command.code() == null ? current.code() : normalizeCode(command.code());
		String name = command.name() == null ? current.name() : command.name();
		int displayOrder = command.displayOrder() == null ? current.displayOrder() : command.displayOrder();
		if (gradeLevelRepository.existsByEstablishmentIdAndCodeAndIdNot(establishmentId, code, gradeLevelId)
			|| gradeLevelRepository.existsByEstablishmentIdAndNameAndIdNot(
				establishmentId, name.strip(), gradeLevelId)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return gradeLevelRepository.update(gradeLevelId, establishmentId, code, name, displayOrder);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public void archiveGradeLevel(UUID establishmentId, UUID gradeLevelId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		gradeLevelRepository.findByIdAndEstablishmentId(gradeLevelId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		if (gradeLevelRepository.hasActiveClasses(establishmentId, gradeLevelId)) {
			throw new SchoolCatalogConflictException();
		}
		gradeLevelRepository.archive(gradeLevelId, establishmentId);
	}

	@Transactional(readOnly = true)
	public List<Subject> findSubjects(UUID establishmentId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		return subjectRepository.findAllByEstablishmentId(establishmentId);
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
		String code = normalizeCode(command.code());
		if (subjectRepository.existsByEstablishmentIdAndCode(establishmentId, code)) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return subjectRepository.save(establishmentId, code, command.name());
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public Subject updateSubject(UUID establishmentId, UUID subjectId,
		UpdateSubjectCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		Subject current = subjectRepository.findByIdAndEstablishmentId(subjectId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		String code = command.code() == null ? current.code() : normalizeCode(command.code());
		String name = command.name() == null ? current.name() : command.name();
		if (subjectRepository.existsByEstablishmentIdAndCodeAndIdNot(establishmentId, code, subjectId)) {
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

	@Transactional
	public SchoolClass createClass(UUID establishmentId, UUID academicYearId,
		CreateSchoolClassCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		gradeLevelRepository.findByIdAndEstablishmentId(command.gradeLevelId(), establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		String code = normalizeCode(command.code());
		if (schoolClassRepository.existsByAcademicYearIdAndCode(academicYearId, code)
			|| schoolClassRepository.existsByAcademicYearIdAndGradeLevelIdAndName(
				academicYearId, command.gradeLevelId(), command.name().strip())) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return schoolClassRepository.save(establishmentId, academicYearId, command.gradeLevelId(),
				code, command.name());
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public SchoolClass updateClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		UpdateSchoolClassCommand command, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		SchoolClass current = findClass(establishmentId, academicYearId, schoolClassId);
		UUID gradeLevelId = command.gradeLevelId() == null ? current.gradeLevelId() : command.gradeLevelId();
		String code = command.code() == null ? current.code() : normalizeCode(command.code());
		String name = command.name() == null ? current.name() : command.name();
		gradeLevelRepository.findByIdAndEstablishmentId(gradeLevelId, establishmentId)
			.orElseThrow(SchoolCatalogNotFoundException::new);
		if (schoolClassRepository.existsByAcademicYearIdAndCodeAndIdNot(academicYearId, code, schoolClassId)
			|| schoolClassRepository.existsByAcademicYearIdAndGradeLevelIdAndName(
				academicYearId, gradeLevelId, name.strip()) &&
				(!gradeLevelId.equals(current.gradeLevelId()) || !name.equals(current.name()))) {
			throw new SchoolCatalogConflictException();
		}
		try {
			return schoolClassRepository.update(schoolClassId, establishmentId, academicYearId,
				gradeLevelId, code, name);
		} catch (DataIntegrityViolationException exception) {
			throw new SchoolCatalogConflictException();
		}
	}

	@Transactional
	public void archiveClass(UUID establishmentId, UUID academicYearId, UUID schoolClassId, EmployeeActor actor) {
		requireEstablishment(establishmentId, actor);
		findAcademicYear(establishmentId, academicYearId);
		findClass(establishmentId, academicYearId, schoolClassId);
		schoolClassRepository.archive(schoolClassId, establishmentId, academicYearId);
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