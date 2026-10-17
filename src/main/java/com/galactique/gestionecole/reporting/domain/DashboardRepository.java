package com.galactique.gestionecole.reporting.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DashboardRepository {
	Dashboard.PlatformSummary findPlatformSummary(Instant now);
	Dashboard.SchoolCounts findSchoolCounts(UUID establishmentId, UUID academicYearId);
	List<Dashboard.ClassEnrollment> findClassEnrollments(UUID establishmentId, UUID academicYearId);
	Dashboard.AttendanceSummary findAttendance(UUID establishmentId, UUID academicYearId, LocalDate date, long enrollments);
	List<Dashboard.CurrencyTotals> findFinance(UUID establishmentId, UUID academicYearId, LocalDate date);
	Dashboard.LearningSummary findLearning(UUID establishmentId, UUID academicYearId);
}
