package com.galactique.gestionecole.reporting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;

public record Dashboard(String scope, Instant generatedAt, PlatformSummary platform, SchoolSummary establishment) {
	public record PlatformSummary(long totalEstablishments, long activeEstablishments, long disabledEstablishments,
		long activeLicenses, long suspendedLicenses, long expiredLicenses, long upcomingLicenses,
		long establishmentsWithoutActiveLicense) { }

	public record SchoolSummary(Establishment establishment, AcademicYear academicYear, LicenseSummary license,
		SchoolCounts counts, List<ClassEnrollment> classes, AttendanceSummary attendance,
		List<CurrencyTotals> finance, LearningSummary learning) {
		public SchoolSummary {
			classes = List.copyOf(classes);
			finance = finance == null ? null : List.copyOf(finance);
		}
	}

	public record LicenseSummary(String state, String offerCode, Instant validFrom, Instant validUntil,
		List<String> features) {
		public LicenseSummary { features = List.copyOf(features); }
	}
	public record SchoolCounts(long students, long employees, long subjects, long classes, long enrollments) { }
	public record ClassEnrollment(UUID id, String code, String name, String series, long students) { }
	public record AttendanceSummary(LocalDate date, long present, long absent, long justifiedAbsent,
		long notRecorded) { }
	public record CurrencyTotals(String currency, BigDecimal amountBilled, BigDecimal amountPaid,
		BigDecimal balance, BigDecimal overdueBalance) { }
	public record LearningSummary(long draftLessons, long publishedLessons, long draftAssignments,
		long publishedAssignments) { }
}
