package com.galactique.gestionecole.attendance.api.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galactique.gestionecole.attendance.api.dto.AttendanceBatchRequest;
import com.galactique.gestionecole.attendance.api.dto.AttendanceHistoryResponse;
import com.galactique.gestionecole.attendance.api.dto.AttendanceSheetResponse;
import com.galactique.gestionecole.attendance.application.AttendanceApplicationService;
import com.galactique.gestionecole.attendance.application.AttendanceEntryCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/establishments/{establishmentId}")
public class AttendanceController {
	private final AttendanceApplicationService attendanceApplicationService;

	public AttendanceController(AttendanceApplicationService attendanceApplicationService) {
		this.attendanceApplicationService = attendanceApplicationService;
	}

	@GetMapping("/school-years/{academicYearId}/classes/{schoolClassId}/attendance")
	@PreAuthorize("hasAuthority('can_attendance_read')")
	public AttendanceSheetResponse getClassAttendance(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@RequestParam(required = false) UUID classroomId,
		Authentication authentication) {
		return AttendanceSheetResponse.from(attendanceApplicationService.findClassAttendance(
			establishmentId, academicYearId, schoolClassId, classroomId, date, actor(authentication, establishmentId)));
	}

	@PutMapping("/school-years/{academicYearId}/classes/{schoolClassId}/attendance/{date}")
	@PreAuthorize("hasAuthority('can_attendance_write')")
	public AttendanceSheetResponse putClassAttendance(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID schoolClassId,
		@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@Valid @RequestBody AttendanceBatchRequest request, Authentication authentication) {
		return AttendanceSheetResponse.from(attendanceApplicationService.recordClassAttendance(
			establishmentId, academicYearId, schoolClassId, request.classroomId(), date,
			request.entries().stream().map(entry -> new AttendanceEntryCommand(entry.studentId(), entry.status(),
				Boolean.TRUE.equals(entry.absenceJustified()), entry.comment(), entry.parentComment(),
				entry.expectedUpdatedAt())).toList(),
			actor(authentication, establishmentId)));
	}

	@GetMapping("/school-years/{academicYearId}/students/{studentId}/attendance")
	@PreAuthorize("hasAuthority('can_attendance_read')")
	public AttendanceHistoryResponse getStudentAttendance(@PathVariable UUID establishmentId,
		@PathVariable UUID academicYearId, @PathVariable UUID studentId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		Authentication authentication) {
		return AttendanceHistoryResponse.from(attendanceApplicationService.findStudentHistory(
			establishmentId, academicYearId, studentId, from, to, actor(authentication, establishmentId)));
	}

	private EmployeeActor actor(Authentication authentication, UUID establishmentId) {
		return EmployeeActor.scopedStaff(authentication.getName(), establishmentId);
	}
}
