package com.galactique.gestionecole.reporting.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.reporting.domain.Dashboard;

public record DashboardResponse(String scope, Instant generatedAt, Dashboard.PlatformSummary platform,
	EstablishmentDashboard establishment) {
	public static DashboardResponse from(Dashboard dashboard) {
		var school = dashboard.establishment();
		return new DashboardResponse(dashboard.scope(), dashboard.generatedAt(), dashboard.platform(), school == null ? null
			: new EstablishmentDashboard(school.establishment().id(), school.establishment().name(),
				school.academicYear() == null ? null : new AcademicYearSummary(school.academicYear().id(),
					school.academicYear().name(), school.academicYear().startsOn(), school.academicYear().endsOn()),
				school.license(), school.counts(), school.classes(), school.attendance(), school.finance(), school.learning()));
	}
	public record AcademicYearSummary(UUID id, String name, LocalDate startsOn, LocalDate endsOn) { }
	public record EstablishmentDashboard(UUID id, String name, AcademicYearSummary academicYear,
		Dashboard.LicenseSummary license, Dashboard.SchoolCounts counts, List<Dashboard.ClassEnrollment> classes,
		Dashboard.AttendanceSummary attendance, List<Dashboard.CurrencyTotals> finance, Dashboard.LearningSummary learning) { }
}
