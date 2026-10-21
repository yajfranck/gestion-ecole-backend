package com.galactique.gestionecole.rollover.api.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.galactique.gestionecole.rollover.api.dto.*;
import com.galactique.gestionecole.rollover.application.SchoolYearRolloverService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}/classrooms/rollover")
public class SchoolYearRolloverController {
	private final SchoolYearRolloverService service;
	public SchoolYearRolloverController(SchoolYearRolloverService service) { this.service = service; }
	@GetMapping
	@PreAuthorize("hasAuthority('can_school_year_rollover')")
	public RolloverPreviewResponse preview(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		Authentication authentication) {
		return RolloverPreviewResponse.from(service.preview(establishmentId, academicYearId, authentication.getName()));
	}
	@PostMapping
	@PreAuthorize("hasAuthority('can_school_year_rollover')")
	public RolloverResponse copy(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@Valid @RequestBody RolloverRequest request, Authentication authentication) {
		return RolloverResponse.from(service.copy(establishmentId, academicYearId, request.toCommand(), authentication.getName()));
	}
}
