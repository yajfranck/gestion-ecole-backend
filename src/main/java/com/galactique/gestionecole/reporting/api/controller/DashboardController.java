package com.galactique.gestionecole.reporting.api.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.reporting.api.dto.DashboardResponse;
import com.galactique.gestionecole.reporting.application.DashboardApplicationService;

@RestController
public class DashboardController {
	private final DashboardApplicationService service;
	public DashboardController(DashboardApplicationService service) { this.service = service; }

	@GetMapping("/api/v1/dashboard")
	@PreAuthorize("hasAuthority('can_dashboard_read')")
	public DashboardResponse getDashboard(@RequestParam(required = false) UUID academicYearId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		Authentication authentication) {
		return DashboardResponse.from(service.getDashboard(authentication.getName(), academicYearId, date));
	}
}
