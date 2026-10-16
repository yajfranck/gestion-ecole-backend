package com.galactique.gestionecole.guardians.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.guardians.application.GuardianChildPortalApplicationService.ChildSummary;

public record GuardianChildSummaryResponse(UUID studentId, String studentNumber, String firstName, String lastName,
	UUID establishmentId, String establishmentName, UUID academicYearId, String academicYearName,
	LocalDate startsOn, LocalDate endsOn, UUID schoolClassId, String schoolClassCode, String schoolClassName,
	List<GuardianCurrencyBalanceResponse> balances) {
	public static GuardianChildSummaryResponse from(ChildSummary summary) {
		return new GuardianChildSummaryResponse(summary.student().id(), summary.student().studentNumber(),
			summary.student().firstName(), summary.student().lastName(), summary.establishment().id(),
			summary.establishment().name(), summary.academicYear().id(), summary.academicYear().name(),
			summary.academicYear().startsOn(), summary.academicYear().endsOn(), summary.schoolClass().id(),
			summary.schoolClass().code(), summary.schoolClass().name(),
			summary.balances().stream().map(GuardianCurrencyBalanceResponse::from).toList());
	}
}
