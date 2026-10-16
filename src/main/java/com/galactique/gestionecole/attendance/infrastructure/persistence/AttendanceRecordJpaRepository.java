package com.galactique.gestionecole.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AttendanceRecordJpaRepository extends JpaRepository<AttendanceRecordEntity, UUID> {
	List<AttendanceRecordEntity> findAllByEnrollmentIdInAndAttendanceDateOrderByEnrollmentIdAsc(
		List<UUID> enrollmentIds, LocalDate attendanceDate);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select attendance from AttendanceRecordEntity attendance "
		+ "where attendance.enrollmentId in :enrollmentIds and attendance.attendanceDate = :attendanceDate")
	List<AttendanceRecordEntity> findAllForUpdate(@Param("enrollmentIds") List<UUID> enrollmentIds,
		@Param("attendanceDate") LocalDate attendanceDate);

	List<AttendanceRecordEntity> findAllByEnrollmentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
		UUID enrollmentId, LocalDate from, LocalDate to);
}