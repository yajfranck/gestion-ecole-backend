package com.galactique.gestionecole.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;
import com.galactique.gestionecole.attendance.domain.AttendanceRepository;
import com.galactique.gestionecole.attendance.domain.AttendanceStatus;

@Repository
public class JpaAttendanceRepositoryAdapter implements AttendanceRepository {
	private final AttendanceRecordJpaRepository jpaRepository;

	public JpaAttendanceRepositoryAdapter(AttendanceRecordJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AttendanceRecord> findByEnrollmentIdsAndDate(List<UUID> enrollmentIds, LocalDate date) {
		if (enrollmentIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository.findAllByEnrollmentIdInAndAttendanceDateOrderByEnrollmentIdAsc(enrollmentIds, date)
			.stream().map(AttendanceRecordEntity::toDomain).toList();
	}

	@Override
	@Transactional
	public List<AttendanceRecord> findByEnrollmentIdsAndDateForUpdate(List<UUID> enrollmentIds, LocalDate date) {
		if (enrollmentIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository.findAllForUpdate(enrollmentIds, date).stream().map(AttendanceRecordEntity::toDomain).toList();
	}

	@Override
	@Transactional
	public AttendanceRecord save(UUID enrollmentId, LocalDate date, AttendanceStatus status,
		boolean absenceJustified, String comment, String parentComment, String username) {
		return jpaRepository.saveAndFlush(new AttendanceRecordEntity(enrollmentId, date, status,
			absenceJustified, comment, parentComment, username)).toDomain();
	}

	@Override
	@Transactional
	public AttendanceRecord update(AttendanceRecord current, AttendanceStatus status,
		boolean absenceJustified, String comment, String parentComment, String username) {
		AttendanceRecordEntity entity = jpaRepository.findById(current.id()).orElseThrow();
		entity.update(status, absenceJustified, comment, parentComment, username);
		return entity.toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AttendanceRecord> findByEnrollmentIdAndDateRange(UUID enrollmentId, LocalDate from, LocalDate to) {
		return jpaRepository.findAllByEnrollmentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
			enrollmentId, from, to).stream().map(AttendanceRecordEntity::toDomain).toList();
	}
}