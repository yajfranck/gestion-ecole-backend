package com.galactique.gestionecole.timetables.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.galactique.gestionecole.timetables.domain.NonWorkingDay;
import com.galactique.gestionecole.timetables.domain.TimetableEntry;
import com.galactique.gestionecole.timetables.domain.TimetableRepository;

@Repository
public class JdbcTimetableRepository implements TimetableRepository {
	private final NamedParameterJdbcTemplate jdbc;
	public JdbcTimetableRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

	@Override
	public List<TimetableEntry> findActiveByEstablishment(UUID establishmentId) {
		return jdbc.query("""
			SELECT t.* FROM timetable_entries t
			JOIN classrooms r ON r.id = t.classroom_id AND r.enabled = TRUE
			JOIN school_classes c ON c.id = t.school_class_id AND c.enabled = TRUE
			JOIN academic_years y ON y.id = t.academic_year_id AND y.enabled = TRUE
			WHERE t.establishment_id = :school AND t.enabled = TRUE
			""", new MapSqlParameterSource("school", establishmentId), (rs, row) -> map(rs));
	}

	@Override
	public TimetableEntry save(TimetableEntry entry) {
		var parameters = parameters(entry);
		int updated = jdbc.update("""
			UPDATE timetable_entries SET subject_id=:subject, teacher_id=:teacher, day_of_week=:day,
			 start_time=:start, end_time=:end, valid_from=:from, valid_until=:until, room_code=:room,
			 updated_by=:actor, updated_at=:now
			WHERE id=:id AND establishment_id=:school AND academic_year_id=:year AND classroom_id=:classroom AND enabled=TRUE
			""", parameters);
		if (updated == 0) {
			jdbc.update("""
				INSERT INTO timetable_entries (id, establishment_id, academic_year_id, school_class_id, classroom_id,
				 subject_id, teacher_id, day_of_week, start_time, end_time, valid_from, valid_until, room_code,
				 enabled, copied_from_id, created_by, updated_by, created_at, updated_at)
				VALUES (:id,:school,:year,:class,:classroom,:subject,:teacher,:day,:start,:end,:from,:until,:room,
				 TRUE,:copied,:creator,:actor,:created,:now)
				""", parameters);
		}
		return entry;
	}

	@Override
	public void archive(TimetableEntry entry, String actor) {
		jdbc.update("""
			UPDATE timetable_entries SET enabled=FALSE, updated_by=:actor, updated_at=:now
			WHERE id=:id AND establishment_id=:school AND classroom_id=:classroom AND enabled=TRUE
			""", new MapSqlParameterSource("id", entry.id()).addValue("school", entry.establishmentId())
			.addValue("classroom", entry.classroomId()).addValue("actor", actor).addValue("now", OffsetDateTime.now()));
	}

	@Override
	public List<NonWorkingDay> findNonWorkingDays(UUID establishmentId, UUID academicYearId) {
		return jdbc.query("""
			SELECT day_date, name FROM timetable_non_working_days
			WHERE establishment_id=:school AND academic_year_id=:year ORDER BY day_date
			""", new MapSqlParameterSource("school", establishmentId).addValue("year", academicYearId),
			(rs, row) -> new NonWorkingDay(rs.getObject("day_date", LocalDate.class), rs.getString("name")));
	}

	@Override
	public void replaceNonWorkingDays(UUID establishmentId, UUID academicYearId, List<NonWorkingDay> days, String actor) {
		var scope = new MapSqlParameterSource("school", establishmentId).addValue("year", academicYearId);
		jdbc.update("DELETE FROM timetable_non_working_days WHERE establishment_id=:school AND academic_year_id=:year", scope);
		for (NonWorkingDay day : days) {
			jdbc.update("""
				INSERT INTO timetable_non_working_days (establishment_id, academic_year_id, day_date, name, updated_by, updated_at)
				VALUES (:school,:year,:date,:name,:actor,:now)
				""", new MapSqlParameterSource("school", establishmentId).addValue("year", academicYearId)
				.addValue("date", day.date()).addValue("name", day.name()).addValue("actor", actor).addValue("now", OffsetDateTime.now()));
		}
	}

	private MapSqlParameterSource parameters(TimetableEntry e) {
		return new MapSqlParameterSource("id", e.id()).addValue("school", e.establishmentId()).addValue("year", e.academicYearId())
			.addValue("class", e.schoolClassId()).addValue("classroom", e.classroomId()).addValue("subject", e.subjectId())
			.addValue("teacher", e.teacherId()).addValue("day", e.dayOfWeek().name()).addValue("start", e.startTime())
			.addValue("end", e.endTime()).addValue("from", e.validFrom()).addValue("until", e.validUntil())
			.addValue("room", e.roomCode()).addValue("copied", e.copiedFromId()).addValue("creator", e.createdBy())
			.addValue("actor", e.updatedBy()).addValue("created", e.createdAt().atOffset(java.time.ZoneOffset.UTC))
			.addValue("now", e.updatedAt().atOffset(java.time.ZoneOffset.UTC));
	}

	private TimetableEntry map(ResultSet rs) throws SQLException {
		return new TimetableEntry(rs.getObject("id", UUID.class), rs.getObject("establishment_id", UUID.class),
			rs.getObject("academic_year_id", UUID.class), rs.getObject("school_class_id", UUID.class),
			rs.getObject("classroom_id", UUID.class), rs.getObject("subject_id", UUID.class), rs.getObject("teacher_id", UUID.class),
			DayOfWeek.valueOf(rs.getString("day_of_week")), rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class),
			rs.getObject("valid_from", LocalDate.class), rs.getObject("valid_until", LocalDate.class), rs.getString("room_code"),
			rs.getBoolean("enabled"), rs.getObject("copied_from_id", UUID.class), rs.getString("created_by"), rs.getString("updated_by"),
			rs.getObject("created_at", OffsetDateTime.class).toInstant(), rs.getObject("updated_at", OffsetDateTime.class).toInstant());
	}
}
