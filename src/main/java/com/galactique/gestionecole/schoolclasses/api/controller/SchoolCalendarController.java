package com.galactique.gestionecole.schoolclasses.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.schoolclasses.api.dto.AcademicPeriodResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.AcademicYearResponse;
import com.galactique.gestionecole.schoolclasses.api.dto.CreateAcademicPeriodRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.CreateAcademicYearRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.UpdateAcademicPeriodRequest;
import com.galactique.gestionecole.schoolclasses.api.dto.UpdateAcademicYearRequest;
import com.galactique.gestionecole.schoolclasses.application.CreateAcademicPeriodCommand;
import com.galactique.gestionecole.schoolclasses.application.CreateAcademicYearCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCalendarApplicationService;
import com.galactique.gestionecole.schoolclasses.application.UpdateAcademicPeriodCommand;
import com.galactique.gestionecole.schoolclasses.application.UpdateAcademicYearCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years")
public class SchoolCalendarController {
	private final SchoolCalendarApplicationService schoolCalendarApplicationService;

	public SchoolCalendarController(SchoolCalendarApplicationService schoolCalendarApplicationService) {
		this.schoolCalendarApplicationService = schoolCalendarApplicationService;
	}

	/** Retourne les années scolaires actives de l’établissement. */
	@GetMapping
	@PreAuthorize("hasAuthority('can_academic_year_read')")
	public List<AcademicYearResponse> getAcademicYears(
		@PathVariable UUID establishmentId, Authentication authentication) {
		return schoolCalendarApplicationService.findYears(establishmentId, toActor(authentication)).stream()
			.map(AcademicYearResponse::from)
			.toList();
	}

	/** Crée une année scolaire avec ses dates de début et de fin. */
	@PostMapping
	@PreAuthorize("hasAuthority('can_academic_year_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public AcademicYearResponse createAcademicYear(@PathVariable UUID establishmentId,
		@Valid @RequestBody CreateAcademicYearRequest request, Authentication authentication) {
		return AcademicYearResponse.from(schoolCalendarApplicationService.createYear(establishmentId,
			new CreateAcademicYearCommand(request.name(), request.startsOn(), request.endsOn()), toActor(authentication)));
	}

	/** Retourne une année scolaire identifiée. */
	@GetMapping("/{academicYearId}")
	@PreAuthorize("hasAuthority('can_academic_year_read')")
	public AcademicYearResponse getAcademicYear(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		Authentication authentication) {
		return AcademicYearResponse.from(
			schoolCalendarApplicationService.findYear(establishmentId, academicYearId, toActor(authentication)));
	}

	/** Modifie le nom ou les dates d’une année scolaire. */
	@PatchMapping("/{academicYearId}")
	@PreAuthorize("hasAuthority('can_academic_year_update')")
	public AcademicYearResponse updateAcademicYear(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @Valid @RequestBody UpdateAcademicYearRequest request,
		Authentication authentication) {
		if (!request.hasChanges()) {
			throw new IllegalArgumentException("At least one academic year field must be provided");
		}
		return AcademicYearResponse.from(schoolCalendarApplicationService.updateYear(establishmentId, academicYearId,
			new UpdateAcademicYearCommand(request.name(), request.startsOn(), request.endsOn()), toActor(authentication)));
	}

	/** Archive une année scolaire sans supprimer son historique. */
	@DeleteMapping("/{academicYearId}")
	@PreAuthorize("hasAuthority('can_academic_year_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveAcademicYear(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		Authentication authentication) {
		schoolCalendarApplicationService.disableYear(establishmentId, academicYearId, toActor(authentication));
	}

	/** Retourne les périodes actives d’une année scolaire. */
	@GetMapping("/{academicYearId}/periods")
	@PreAuthorize("hasAuthority('can_academic_period_read')")
	public List<AcademicPeriodResponse> getAcademicPeriods(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return schoolCalendarApplicationService.findPeriods(establishmentId, academicYearId, toActor(authentication)).stream()
			.map(AcademicPeriodResponse::from)
			.toList();
	}

	/** Crée une période dans les dates de l’année scolaire. */
	@PostMapping("/{academicYearId}/periods")
	@PreAuthorize("hasAuthority('can_academic_period_create')")
	@ResponseStatus(HttpStatus.CREATED)
	public AcademicPeriodResponse createAcademicPeriod(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @Valid @RequestBody CreateAcademicPeriodRequest request,
		Authentication authentication) {
		return AcademicPeriodResponse.from(schoolCalendarApplicationService.createPeriod(establishmentId, academicYearId,
			new CreateAcademicPeriodCommand(request.name(), request.startsOn(), request.endsOn()), toActor(authentication)));
	}

	/** Modifie le nom ou les dates d’une période scolaire. */
	@PatchMapping("/{academicYearId}/periods/{periodId}")
	@PreAuthorize("hasAuthority('can_academic_period_update')")
	public AcademicPeriodResponse updateAcademicPeriod(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID periodId,
		@Valid @RequestBody UpdateAcademicPeriodRequest request, Authentication authentication) {
		if (!request.hasChanges()) {
			throw new IllegalArgumentException("At least one academic period field must be provided");
		}
		return AcademicPeriodResponse.from(schoolCalendarApplicationService.updatePeriod(establishmentId,
			academicYearId, periodId,
			new UpdateAcademicPeriodCommand(request.name(), request.startsOn(), request.endsOn()),
			toActor(authentication)));
	}

	/** Archive une période sans supprimer son historique. */
	@DeleteMapping("/{academicYearId}/periods/{periodId}")
	@PreAuthorize("hasAuthority('can_academic_period_delete')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveAcademicPeriod(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID periodId, Authentication authentication) {
		schoolCalendarApplicationService.disablePeriod(establishmentId, academicYearId, periodId,
			toActor(authentication));
	}

	private EmployeeActor toActor(Authentication authentication) {
		return new EmployeeActor(authentication.getName());
	}
}
