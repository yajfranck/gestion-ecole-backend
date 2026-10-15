package com.galactique.gestionecole.students.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.students.application.CreateStudentCommand;
import com.galactique.gestionecole.students.application.StudentApplicationService;
import com.galactique.gestionecole.students.application.UpdateStudentCommand;
import com.galactique.gestionecole.students.domain.Student;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/students")
public class StudentController {
	private final StudentApplicationService studentApplicationService;

	public StudentController(StudentApplicationService studentApplicationService) {
		this.studentApplicationService = studentApplicationService;
	}

	/** Retourne les élèves actifs d’un établissement. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_student_read')")
	public List<StudentResponse> getStudents(@PathVariable UUID establishmentId, Authentication authentication) {
		return studentApplicationService.findAll(establishmentId, toActor(authentication)).stream()
			.map(StudentResponse::from)
			.toList();
	}

	/** Crée une fiche élève dans l’établissement. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_student_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public StudentResponse createStudent(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreateStudentRequest request, Authentication authentication) {
		Student student = studentApplicationService.create(establishmentId,
			new CreateStudentCommand(request.firstName(), request.lastName()),
			toActor(authentication));
		return StudentResponse.from(student);
	}

	/** Retourne la fiche d’un élève identifié. */
	@GetMapping("/{studentId}")
	@PreAuthorize("hasAuthority('can_student_read')")
	public StudentResponse getStudent(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		Authentication authentication) {
		return StudentResponse.from(studentApplicationService.findById(
			establishmentId, studentId, toActor(authentication)));
	}

	/** Modifie partiellement les informations d’une fiche élève. */
	@PatchMapping("/{studentId}")
	@PreAuthorize("hasAuthority('can_student_update')")
	public StudentResponse updateStudent(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@Valid @RequestBody UpdateStudentRequest request, Authentication authentication) {
		if (!request.hasChanges()) {
			throw new IllegalArgumentException("At least one student field must be provided");
		}
		Student student = studentApplicationService.update(establishmentId, studentId,
			new UpdateStudentCommand(request.firstName(), request.lastName()),
			toActor(authentication));
		return StudentResponse.from(student);
	}

	/** Archive une fiche élève sans supprimer son historique. */
	@DeleteMapping("/{studentId}")
	@PreAuthorize("hasAuthority('can_student_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveStudent(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		Authentication authentication) {
		studentApplicationService.archive(establishmentId, studentId, toActor(authentication));
	}

	private EmployeeActor toActor(Authentication authentication) {
		boolean globalAdministrator = hasAuthority(authentication, "ADMIN_ACCESS");
		boolean establishmentAdministrator = hasAuthority(authentication, "ESTABLISHMENT_STAFF_MANAGEMENT");
		return new EmployeeActor(authentication.getName(), globalAdministrator, establishmentAdministrator);
	}

	private boolean hasAuthority(Authentication authentication, String expectedAuthority) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority != null && expectedAuthority.equals(authority.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}