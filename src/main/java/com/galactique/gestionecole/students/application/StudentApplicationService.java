package com.galactique.gestionecole.students.application;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.students.domain.Student;
import com.galactique.gestionecole.students.domain.StudentRepository;

@Service
public class StudentApplicationService {
	private final StudentRepository studentRepository;
	private final EmployeeApplicationService employeeApplicationService;
	private final LicenseApplicationService licenseApplicationService;

	public StudentApplicationService(
		StudentRepository studentRepository,
		EmployeeApplicationService employeeApplicationService,
		LicenseApplicationService licenseApplicationService) {
		this.studentRepository = studentRepository;
		this.employeeApplicationService = employeeApplicationService;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Transactional
	public Student create(UUID establishmentId, CreateStudentCommand command, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		try {
			return studentRepository.save(establishmentId, command.firstName(), command.lastName(), command.dateOfBirth());
		} catch (DataIntegrityViolationException exception) {
			throw new StudentConflictException();
		}
	}

	@Transactional(readOnly = true)
	public List<Student> findAll(UUID establishmentId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		return studentRepository.findAllByEstablishmentId(establishmentId);
	}

	@Transactional(readOnly = true)
	public Student findById(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		return findActiveStudent(establishmentId, studentId, actor);
	}

	@Transactional(readOnly = true)
	public List<Student> findActiveByIds(UUID establishmentId, List<UUID> studentIds, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		return studentRepository.findActiveByEstablishmentIdAndIds(establishmentId, studentIds);
	}

	@Transactional(readOnly = true)
	public Student findIncludingArchived(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		return studentRepository.findIncludingArchivedByIdAndEstablishmentId(studentId, establishmentId)
			.orElseThrow(StudentNotFoundException::new);
	}

	private Student findActiveStudent(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		requireAccessibleEstablishment(establishmentId, actor);
		return studentRepository.findByIdAndEstablishmentId(studentId, establishmentId)
			.filter(Student::enabled)
			.orElseThrow(StudentNotFoundException::new);
	}

	@Transactional
	public Student update(UUID establishmentId, UUID studentId, UpdateStudentCommand command, EmployeeActor actor) {
		Student current = findActiveStudent(establishmentId, studentId, actor);
		String firstName = command.firstName() == null ? current.firstName() : command.firstName();
		String lastName = command.lastName() == null ? current.lastName() : command.lastName();
		return studentRepository.updateProfile(studentId, establishmentId, firstName, lastName,
			command.dateOfBirth() == null ? current.dateOfBirth() : command.dateOfBirth());
	}

	@Transactional
	public void archive(UUID establishmentId, UUID studentId, EmployeeActor actor) {
		findActiveStudent(establishmentId, studentId, actor);
		studentRepository.archive(studentId, establishmentId);
	}

	private Establishment requireAccessibleEstablishment(UUID establishmentId, EmployeeActor actor) {
		if (!licenseApplicationService.canAccessFeature(establishmentId, LicenseFeature.STUDENT_RECORDS,
			java.time.Instant.now())) {
			throw new StudentAccessDeniedException();
		}
		List<Establishment> accessibleEstablishments;
		try {
			accessibleEstablishments = employeeApplicationService.findEstablishments(actor);
		} catch (EmployeeAccessDeniedException exception) {
			throw new StudentAccessDeniedException();
		}
		return accessibleEstablishments.stream()
			.filter(Establishment::enabled)
			.filter(establishment -> establishment.id().equals(establishmentId))
			.findFirst()
			.orElseThrow(StudentNotFoundException::new);
	}
}
