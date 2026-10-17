package com.galactique.gestionecole.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.licenses.domain.LicenseRepository;
import com.galactique.gestionecole.licenses.domain.LicenseState;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;
import com.galactique.gestionecole.reporting.domain.Dashboard;
import com.galactique.gestionecole.reporting.domain.DashboardRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.UserAccountRepository;

@Service
public class DashboardApplicationService {
	private final UserAccountRepository accounts;
	private final EmployeeRepository employees;
	private final EstablishmentRepository establishments;
	private final AcademicYearRepository years;
	private final LicenseRepository licenses;
	private final DashboardRepository dashboard;

	public DashboardApplicationService(UserAccountRepository accounts, EmployeeRepository employees,
		EstablishmentRepository establishments, AcademicYearRepository years, LicenseRepository licenses,
		DashboardRepository dashboard) {
		this.accounts = accounts;
		this.employees = employees;
		this.establishments = establishments;
		this.years = years;
		this.licenses = licenses;
		this.dashboard = dashboard;
	}

	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public Dashboard getDashboard(String username, UUID academicYearId, LocalDate requestedDate) {
		var account = accounts.findByUsername(username).filter(a -> a.enabled() && !a.locked())
			.orElseThrow(DashboardAccessDeniedException::new);
		List<Role> roles = account.roles().stream().filter(Role::enabled).toList();
		if (roles.stream().flatMap(role -> role.permissions().stream())
			.noneMatch(permission -> "can_dashboard_read".equals(permission.code()))) {
			throw new DashboardAccessDeniedException();
		}
		Instant now = Instant.now();
		if (roles.stream().anyMatch(role -> "ADMIN".equals(role.code()))) {
			if (academicYearId != null || requestedDate != null) { throw new DashboardValidationException(); }
			return new Dashboard("PLATFORM", now, dashboard.findPlatformSummary(now), null);
		}
		if (roles.stream().noneMatch(role -> "ESTABLISHMENT_ADMIN".equals(role.code()))) {
			throw new DashboardAccessDeniedException();
		}
		Employee employee = employees.findByEmail(account.username()).filter(Employee::enabled)
			.filter(Employee::establishmentAdmin).filter(e -> account.id().equals(e.userAccountId()))
			.orElseThrow(DashboardAccessDeniedException::new);
		Establishment school = establishments.findById(employee.establishmentId()).filter(Establishment::enabled)
			.orElseThrow(DashboardNotFoundException::new);
		LocalDate today = now.atOffset(ZoneOffset.UTC).toLocalDate();
		AcademicYear year = resolveYear(school.id(), academicYearId, today);
		if (requestedDate != null && (year == null || requestedDate.isBefore(year.startsOn())
			|| requestedDate.isAfter(year.endsOn()))) { throw new DashboardValidationException(); }
		LocalDate date = requestedDate != null ? requestedDate : year == null ? today
			: today.isBefore(year.startsOn()) ? year.startsOn() : today.isAfter(year.endsOn()) ? year.endsOn() : today;
		List<PlatformLicense> schoolLicenses = licenses.findLicenses(school.id());
		PlatformLicense selected = selectLicense(schoolLicenses, now);
		Set<LicenseFeature> features = selected != null && selected.stateAt(now) == LicenseState.ACTIVE
			? licenses.findEnabledOffer(selected.offerId()).map(offer -> offer.features()).orElse(Set.of()) : Set.of();
		Dashboard.LicenseSummary license = selected == null
			? new Dashboard.LicenseSummary("UNLICENSED", null, null, null, List.of())
			: new Dashboard.LicenseSummary(selected.stateAt(now).name(), selected.offerCode(), selected.validFrom(),
				selected.validUntil(), features.stream().map(Enum::name).sorted().toList());
		UUID yearId = year == null ? null : year.id();
		var counts = dashboard.findSchoolCounts(school.id(), yearId);
		return new Dashboard("ESTABLISHMENT", now, null, new Dashboard.SchoolSummary(school, year, license,
			counts, dashboard.findClassEnrollments(school.id(), yearId),
			features.contains(LicenseFeature.ATTENDANCE) ? dashboard.findAttendance(school.id(), yearId, date, counts.enrollments()) : null,
			features.contains(LicenseFeature.TUITION) ? dashboard.findFinance(school.id(), yearId, date) : null,
			features.contains(LicenseFeature.LEARNING_CONTENT) ? dashboard.findLearning(school.id(), yearId) : null));
	}

	private AcademicYear resolveYear(UUID schoolId, UUID requestedId, LocalDate today) {
		if (requestedId != null) {
			return years.findByIdAndEstablishmentId(requestedId, schoolId).filter(AcademicYear::enabled)
				.orElseThrow(DashboardNotFoundException::new);
		}
		List<AcademicYear> activeYears = years.findAllByEstablishmentId(schoolId).stream()
			.filter(AcademicYear::enabled).sorted(Comparator.comparing(AcademicYear::startsOn).reversed()
				.thenComparing(AcademicYear::id)).toList();
		return activeYears.stream().filter(y -> !today.isBefore(y.startsOn()) && !today.isAfter(y.endsOn()))
			.findFirst().orElse(activeYears.isEmpty() ? null : activeYears.getFirst());
	}

	private PlatformLicense selectLicense(List<PlatformLicense> items, Instant now) {
		Comparator<PlatformLicense> byStart = Comparator.comparing(PlatformLicense::validFrom).thenComparing(PlatformLicense::id);
		return items.stream().filter(l -> !l.validFrom().isAfter(now) && (l.validUntil() == null || l.validUntil().isAfter(now)))
			.max(byStart).orElseGet(() -> items.stream().filter(l -> l.validFrom().isAfter(now)).min(byStart)
				.orElseGet(() -> items.stream().max(byStart).orElse(null)));
	}
}
