package com.galactique.gestionecole.reporting.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.galactique.gestionecole.reporting.domain.Dashboard;
import com.galactique.gestionecole.reporting.domain.DashboardRepository;

@Repository
public class JdbcDashboardRepository implements DashboardRepository {
	private final NamedParameterJdbcTemplate jdbc;
	public JdbcDashboardRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

	@Override
	public Dashboard.PlatformSummary findPlatformSummary(Instant now) {
		return jdbc.queryForObject("""
			SELECT
			 (SELECT COUNT(*) FROM establishments) AS total,
			 (SELECT COUNT(*) FROM establishments WHERE enabled = TRUE) AS active,
			 (SELECT COUNT(*) FROM establishments WHERE enabled = FALSE) AS disabled,
			 (SELECT COUNT(*) FROM platform_licenses WHERE valid_from <= :now
			   AND (valid_until IS NULL OR valid_until > :now) AND suspended_at IS NULL) AS active_licenses,
			 (SELECT COUNT(*) FROM platform_licenses WHERE valid_from <= :now
			   AND (valid_until IS NULL OR valid_until > :now) AND suspended_at IS NOT NULL) AS suspended,
			 (SELECT COUNT(*) FROM platform_licenses WHERE valid_until <= :now) AS expired,
			 (SELECT COUNT(*) FROM platform_licenses WHERE valid_from > :now) AS upcoming,
			 (SELECT COUNT(*) FROM establishments e WHERE e.enabled = TRUE AND NOT EXISTS (
			   SELECT 1 FROM platform_licenses l JOIN license_offers o ON o.id = l.offer_id AND o.enabled = TRUE
			   WHERE l.establishment_id = e.id AND l.valid_from <= :now
			   AND (l.valid_until IS NULL OR l.valid_until > :now) AND l.suspended_at IS NULL)) AS unlicensed
			""", new MapSqlParameterSource("now", Timestamp.from(now)), (rs, row) -> new Dashboard.PlatformSummary(
				rs.getLong("total"), rs.getLong("active"), rs.getLong("disabled"), rs.getLong("active_licenses"),
				rs.getLong("suspended"), rs.getLong("expired"), rs.getLong("upcoming"), rs.getLong("unlicensed")));
	}

	@Override
	public Dashboard.SchoolCounts findSchoolCounts(UUID establishmentId, UUID academicYearId) {
		return jdbc.queryForObject("""
			SELECT
			 (SELECT COUNT(*) FROM students WHERE establishment_id = :school AND enabled = TRUE) AS students,
			 (SELECT COUNT(*) FROM employees WHERE establishment_id = :school AND enabled = TRUE) AS employees,
			 (SELECT COUNT(*) FROM subjects WHERE establishment_id = :school AND enabled = TRUE) AS subjects,
			 (SELECT COUNT(*) FROM school_classes WHERE establishment_id = :school
			   AND academic_year_id = :year AND enabled = TRUE) AS classes,
			 (SELECT COUNT(*) FROM enrollments e JOIN students s ON s.id = e.student_id AND s.enabled = TRUE
			   JOIN school_classes c ON c.id = e.school_class_id AND c.enabled = TRUE
			   WHERE e.establishment_id = :school AND e.academic_year_id = :year) AS enrollments
			""", parameters(establishmentId, academicYearId), (rs, row) -> new Dashboard.SchoolCounts(
				rs.getLong("students"), rs.getLong("employees"), rs.getLong("subjects"),
				rs.getLong("classes"), rs.getLong("enrollments")));
	}

	@Override
	public List<Dashboard.ClassEnrollment> findClassEnrollments(UUID establishmentId, UUID academicYearId) {
		return jdbc.query("""
			SELECT c.id, c.code, c.name, c.series, COUNT(s.id) AS students FROM school_classes c
			LEFT JOIN enrollments e ON e.school_class_id = c.id AND e.establishment_id = c.establishment_id
			 AND e.academic_year_id = c.academic_year_id
			LEFT JOIN students s ON s.id = e.student_id AND s.enabled = TRUE
			WHERE c.establishment_id = :school AND c.academic_year_id = :year AND c.enabled = TRUE
			GROUP BY c.id, c.code, c.name, c.series ORDER BY c.name, c.id
			""", parameters(establishmentId, academicYearId), (rs, row) -> new Dashboard.ClassEnrollment(
				rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("series"),
				rs.getLong("students")));
	}

	@Override
	public Dashboard.AttendanceSummary findAttendance(UUID establishmentId, UUID academicYearId,
		LocalDate date, long enrollments) {
		return jdbc.queryForObject("""
			SELECT COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present,
			 COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent,
			 COALESCE(SUM(CASE WHEN a.status = 'ABSENT' AND a.absence_justified = TRUE THEN 1 ELSE 0 END), 0) AS justified
			FROM attendance_records a JOIN enrollments e ON e.id = a.enrollment_id
			JOIN students s ON s.id = e.student_id AND s.enabled = TRUE
			JOIN school_classes c ON c.id = e.school_class_id AND c.enabled = TRUE
			WHERE e.establishment_id = :school AND e.academic_year_id = :year AND a.attendance_date = :date
			""", parameters(establishmentId, academicYearId).addValue("date", date), (rs, row) ->
				new Dashboard.AttendanceSummary(date, rs.getLong("present"), rs.getLong("absent"),
					rs.getLong("justified"), Math.max(0, enrollments - rs.getLong("present") - rs.getLong("absent"))));
	}

	@Override
	public List<Dashboard.CurrencyTotals> findFinance(UUID establishmentId, UUID academicYearId, LocalDate date) {
		return jdbc.query("""
			SELECT c.currency, SUM(c.amount) AS billed, SUM(COALESCE(p.paid, 0)) AS paid,
			 SUM(c.amount - COALESCE(p.paid, 0)) AS balance,
			 SUM(CASE WHEN c.due_date < :date THEN c.amount - COALESCE(p.paid, 0) ELSE 0 END) AS overdue
			FROM student_fee_charges c LEFT JOIN (
			 SELECT charge_id, establishment_id, SUM(amount) AS paid FROM tuition_payments
			 WHERE establishment_id = :school GROUP BY charge_id, establishment_id
			) p ON p.charge_id = c.id AND p.establishment_id = c.establishment_id
			WHERE c.establishment_id = :school AND c.academic_year_id = :year
			GROUP BY c.currency ORDER BY c.currency
			""", parameters(establishmentId, academicYearId).addValue("date", date), (rs, row) ->
				new Dashboard.CurrencyTotals(rs.getString("currency"), rs.getBigDecimal("billed"),
					rs.getBigDecimal("paid"), rs.getBigDecimal("balance"), rs.getBigDecimal("overdue")));
	}

	@Override
	public Dashboard.LearningSummary findLearning(UUID establishmentId, UUID academicYearId) {
		return jdbc.queryForObject("""
			SELECT
			 COALESCE(SUM(CASE WHEN l.type = 'LESSON' AND l.status = 'DRAFT' THEN 1 ELSE 0 END), 0) AS draft_lessons,
			 COALESCE(SUM(CASE WHEN l.type = 'LESSON' AND l.status = 'PUBLISHED' THEN 1 ELSE 0 END), 0) AS published_lessons,
			 COALESCE(SUM(CASE WHEN l.type = 'ASSIGNMENT' AND l.status = 'DRAFT' THEN 1 ELSE 0 END), 0) AS draft_assignments,
			 COALESCE(SUM(CASE WHEN l.type = 'ASSIGNMENT' AND l.status = 'PUBLISHED' THEN 1 ELSE 0 END), 0) AS published_assignments
			FROM learning_contents l JOIN school_classes c ON c.id = l.school_class_id AND c.enabled = TRUE
			WHERE l.establishment_id = :school AND l.academic_year_id = :year
			""", parameters(establishmentId, academicYearId), (rs, row) -> new Dashboard.LearningSummary(
				rs.getLong("draft_lessons"), rs.getLong("published_lessons"),
				rs.getLong("draft_assignments"), rs.getLong("published_assignments")));
	}

	private MapSqlParameterSource parameters(UUID establishmentId, UUID academicYearId) {
		return new MapSqlParameterSource("school", establishmentId).addValue("year", academicYearId);
	}
}
