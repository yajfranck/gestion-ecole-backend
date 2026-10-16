package com.galactique.gestionecole.attendance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository {
	List<AttendanceRecord> findByEnrollmentIdsAndDate(List<UUID> enrollmentIds, LocalDate date);

	List<AttendanceRecord> findByEnrollmentIdsAndDateForUpdate(List<UUID> enrollmentIds, LocalDate date);

	AttendanceRecord save(UUID enrollmentId, LocalDate date, AttendanceStatus status,
		boolean absenceJustified, String comment, String parentComment, String username);

	AttendanceRecord update(AttendanceRecord current, AttendanceStatus status,
		boolean absenceJustified, String comment, String parentComment, String username);

	List<AttendanceRecord> findByEnrollmentIdAndDateRange(UUID enrollmentId, LocalDate from, LocalDate to);
}