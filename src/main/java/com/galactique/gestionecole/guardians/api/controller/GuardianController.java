package com.galactique.gestionecole.guardians.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.guardians.api.dto.AcceptGuardianInvitationRequest;
import com.galactique.gestionecole.guardians.api.dto.GuardianChildResponse;
import com.galactique.gestionecole.guardians.api.dto.GuardianLinkResponse;
import com.galactique.gestionecole.guardians.api.dto.GuardianResponse;
import com.galactique.gestionecole.guardians.api.dto.LinkGuardianRequest;
import com.galactique.gestionecole.guardians.application.GuardianApplicationService;
import jakarta.validation.Valid;

@RestController
public class GuardianController {
	private final GuardianApplicationService guardianApplicationService;

	public GuardianController(GuardianApplicationService guardianApplicationService) {
		this.guardianApplicationService = guardianApplicationService;
	}

	@GetMapping("/api/v1/establishments/{establishmentId}/students/{studentId}/guardians")
	@PreAuthorize("hasAuthority('can_guardian_read')")
	public List<GuardianResponse> getStudentGuardians(@PathVariable UUID establishmentId,
		@PathVariable UUID studentId, Authentication authentication) {
		return guardianApplicationService.findByStudent(establishmentId, studentId, toActor(authentication))
			.stream().map(GuardianResponse::from).toList();
	}

	@PostMapping("/api/v1/establishments/{establishmentId}/students/{studentId}/guardians")
	@PreAuthorize("hasAuthority('can_guardian_link')")
	@ResponseStatus(HttpStatus.CREATED)
	public GuardianLinkResponse linkGuardian(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@Valid @RequestBody LinkGuardianRequest request, Authentication authentication) {
		return GuardianLinkResponse.from(guardianApplicationService.linkToStudent(establishmentId, studentId,
			request.firstName(), request.lastName(), request.email(), toActor(authentication)));
	}

	@DeleteMapping("/api/v1/establishments/{establishmentId}/students/{studentId}/guardians/{guardianId}")
	@PreAuthorize("hasAuthority('can_guardian_unlink')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unlinkGuardian(@PathVariable UUID establishmentId, @PathVariable UUID studentId,
		@PathVariable UUID guardianId, Authentication authentication) {
		guardianApplicationService.unlinkFromStudent(establishmentId, studentId, guardianId, toActor(authentication));
	}

	@PostMapping("/api/v1/guardians/invitations/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void acceptInvitation(@Valid @RequestBody AcceptGuardianInvitationRequest request) {
		guardianApplicationService.acceptInvitation(request.token(), request.password());
	}

	@GetMapping("/api/v1/guardians/me/children")
	@PreAuthorize("hasAuthority('can_guardian_read_children')")
	public List<GuardianChildResponse> getMyChildren(Authentication authentication) {
		return guardianApplicationService.findChildren(authentication.getName()).stream()
			.map(GuardianChildResponse::from).toList();
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName(), hasAuthority(authentication, "ADMIN_ACCESS"),
			hasAuthority(authentication, "ESTABLISHMENT_STAFF_MANAGEMENT"));
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