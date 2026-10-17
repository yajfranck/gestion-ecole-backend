package com.galactique.gestionecole.timetables.api.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.galactique.gestionecole.timetables.api.dto.CopyTimetableRequest;
import com.galactique.gestionecole.timetables.api.dto.NonWorkingDaysRequest;
import com.galactique.gestionecole.timetables.api.dto.NonWorkingDayResponse;
import com.galactique.gestionecole.timetables.api.dto.TimetableEntryRequest;
import com.galactique.gestionecole.timetables.api.dto.TimetableEntryResponse;
import com.galactique.gestionecole.timetables.api.dto.TimetableDayResponse;
import com.galactique.gestionecole.timetables.api.dto.TimetableWeekResponse;
import com.galactique.gestionecole.timetables.application.TimetableApplicationService;
import com.galactique.gestionecole.timetables.domain.NonWorkingDay;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}/school-years/{academicYearId}")
public class TimetableController {
	private static final String BASE = "/classes/{schoolClassId}/classrooms/{classroomId}/timetable";
	private final TimetableApplicationService service;
	public TimetableController(TimetableApplicationService service) { this.service = service; }

	@GetMapping(BASE)
	@PreAuthorize("hasAuthority('can_timetable_read')")
	public List<TimetableEntryResponse> list(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, @RequestParam(required = false) DayOfWeek dayOfWeek,
		Authentication authentication) {
		return service.list(establishmentId, academicYearId, schoolClassId, classroomId, dayOfWeek, authentication.getName())
			.stream().map(TimetableEntryResponse::from).toList();
	}

	@GetMapping(BASE + "/{entryId}")
	@PreAuthorize("hasAuthority('can_timetable_read')")
	public TimetableEntryResponse get(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, @PathVariable UUID entryId, Authentication authentication) {
		return TimetableEntryResponse.from(service.get(establishmentId, academicYearId, schoolClassId, classroomId, entryId, authentication.getName()));
	}

	@PostMapping(BASE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('can_timetable_create')")
	public TimetableEntryResponse create(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, @Valid @RequestBody TimetableEntryRequest request,
		Authentication authentication) {
		return TimetableEntryResponse.from(service.save(establishmentId, academicYearId, schoolClassId, classroomId, null,
			request.toCommand(), authentication.getName()));
	}

	@PutMapping(BASE + "/{entryId}")
	@PreAuthorize("hasAuthority('can_timetable_update')")
	public TimetableEntryResponse update(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, @PathVariable UUID entryId,
		@Valid @RequestBody TimetableEntryRequest request, Authentication authentication) {
		return TimetableEntryResponse.from(service.save(establishmentId, academicYearId, schoolClassId, classroomId, entryId,
			request.toCommand(), authentication.getName()));
	}

	@DeleteMapping(BASE + "/{entryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('can_timetable_delete')")
	public void archive(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, @PathVariable UUID entryId, Authentication authentication) {
		service.archive(establishmentId, academicYearId, schoolClassId, classroomId, entryId, authentication.getName());
	}

	@PostMapping(BASE + "/copy")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAuthority('can_timetable_copy') and (#request.replaceExisting() == null "
		+ "or !#request.replaceExisting() or hasAuthority('can_timetable_delete'))")
	public List<TimetableEntryResponse> copy(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId, @Valid @RequestBody CopyTimetableRequest request,
		Authentication authentication) {
		return service.copy(establishmentId, academicYearId, schoolClassId, classroomId, request.toCommand(), authentication.getName())
			.stream().map(TimetableEntryResponse::from).toList();
	}

	@GetMapping(BASE + "/day")
	@PreAuthorize("hasAuthority('can_timetable_read')")
	public TimetableDayResponse day(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Authentication authentication) {
		return TimetableDayResponse.from(service.day(establishmentId, academicYearId, schoolClassId, classroomId, date, authentication.getName()));
	}

	@GetMapping(BASE + "/week")
	@PreAuthorize("hasAuthority('can_timetable_read')")
	public TimetableWeekResponse week(@PathVariable UUID establishmentId, @PathVariable UUID academicYearId,
		@PathVariable UUID schoolClassId, @PathVariable UUID classroomId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Authentication authentication) {
		return TimetableWeekResponse.from(service.week(establishmentId, academicYearId, schoolClassId, classroomId, date, authentication.getName()));
	}

	@GetMapping("/timetable/non-working-days")
	@PreAuthorize("hasAuthority('can_timetable_read')")
	public List<NonWorkingDayResponse> nonWorkingDays(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, Authentication authentication) {
		return service.nonWorkingDays(establishmentId, academicYearId, authentication.getName()).stream().map(NonWorkingDayResponse::from).toList();
	}

	@PutMapping("/timetable/non-working-days")
	@PreAuthorize("hasAuthority('can_timetable_update')")
	public List<NonWorkingDayResponse> replaceNonWorkingDays(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @Valid @RequestBody NonWorkingDaysRequest request, Authentication authentication) {
		return service.replaceNonWorkingDays(establishmentId, academicYearId,
			request.days().stream().map(day -> new NonWorkingDay(day.date(), day.name())).toList(), authentication.getName())
			.stream().map(NonWorkingDayResponse::from).toList();
	}
}
