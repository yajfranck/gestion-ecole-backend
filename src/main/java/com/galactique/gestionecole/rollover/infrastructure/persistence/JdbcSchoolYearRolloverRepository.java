package com.galactique.gestionecole.rollover.infrastructure.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.galactique.gestionecole.rollover.domain.SchoolYearRolloverRepository;
import com.galactique.gestionecole.rollover.domain.SourceClassroom;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.Classroom;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;

@Repository
public class JdbcSchoolYearRolloverRepository implements SchoolYearRolloverRepository {
	private final NamedParameterJdbcTemplate jdbc;
	public JdbcSchoolYearRolloverRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
	@Override
	public List<AcademicYear> findYears(UUID school) {
		return jdbc.query("SELECT * FROM academic_years WHERE establishment_id=:school", new MapSqlParameterSource("school", school),
			(rs, row) -> new AcademicYear(rs.getObject("id", UUID.class), school, rs.getString("name"),
				rs.getObject("starts_on", LocalDate.class), rs.getObject("ends_on", LocalDate.class), rs.getBoolean("enabled"),
				rs.getObject("created_at", OffsetDateTime.class).toInstant(), rs.getObject("updated_at", OffsetDateTime.class).toInstant()));
	}
	@Override
	public void lockYears(UUID school, UUID sourceYear, UUID targetYear) {
		jdbc.query("SELECT id FROM academic_years WHERE establishment_id=:school AND id IN (:years) ORDER BY id FOR UPDATE",
			new MapSqlParameterSource("school", school).addValue("years", List.of(sourceYear, targetYear)), (rs, row) -> rs.getObject("id", UUID.class));
	}
	@Override
	public List<SourceClassroom> findClassrooms(UUID school, UUID year) {
		return jdbc.query("""
			SELECT r.*, c.code AS class_code, c.name AS class_name, c.series AS class_series,
			 c.enabled AS class_enabled, c.created_at AS class_created, c.updated_at AS class_updated
			FROM classrooms r JOIN school_classes c ON c.id=r.school_class_id
			WHERE r.establishment_id=:school AND r.academic_year_id=:year ORDER BY c.code, r.code, r.id
			""", new MapSqlParameterSource("school", school).addValue("year", year), (rs, row) -> {
				UUID classId = rs.getObject("school_class_id", UUID.class);
				return new SourceClassroom(new Classroom(rs.getObject("id", UUID.class), school, year, classId,
					rs.getString("code"), rs.getString("name"), rs.getInt("max_students"), rs.getBoolean("enabled"),
					rs.getObject("created_at", OffsetDateTime.class).toInstant(), rs.getObject("updated_at", OffsetDateTime.class).toInstant()),
					new SchoolClass(classId, school, year, rs.getString("class_code"), rs.getString("class_name"), rs.getString("class_series"),
						rs.getBoolean("class_enabled"), rs.getObject("class_created", OffsetDateTime.class).toInstant(), rs.getObject("class_updated", OffsetDateTime.class).toInstant()));
			});
	}
	@Override public Map<UUID, UUID> classLinks(UUID school, UUID sourceYear, UUID targetYear) { return links("school_year_rollover_classes", school, sourceYear, targetYear); }
	@Override public Map<UUID, UUID> classroomLinks(UUID school, UUID sourceYear, UUID targetYear) { return links("school_year_rollover_classrooms", school, sourceYear, targetYear); }
	@Override public Map<UUID, UUID> feeLinks(UUID school, UUID sourceYear, UUID targetYear) { return links("school_year_rollover_fees", school, sourceYear, targetYear); }
	@Override public void linkClass(UUID school, UUID sourceYear, UUID targetYear, UUID source, UUID target, String actor) {
		link("school_year_rollover_classes", scope(school, sourceYear, targetYear, source, target, actor));
	}
	@Override public void linkFee(UUID school, UUID sourceYear, UUID targetYear, UUID source, UUID target, String actor) {
		link("school_year_rollover_fees", scope(school, sourceYear, targetYear, source, target, actor));
	}
	@Override public void linkClassroom(Classroom source, Classroom target, String actor) {
		var parameters = scope(source.establishmentId(), source.academicYearId(), target.academicYearId(), source.id(), target.id(), actor)
			.addValue("sourceClass", source.schoolClassId()).addValue("targetClass", target.schoolClassId());
		jdbc.update("""
			INSERT INTO school_year_rollover_classrooms (establishment_id, source_academic_year_id, target_academic_year_id,
			 source_class_id, target_class_id, source_id, target_id, created_by, created_at)
			VALUES (:school,:sourceYear,:targetYear,:sourceClass,:targetClass,:source,:target,:actor,:now)
			""", parameters);
	}
	private Map<UUID, UUID> links(String table, UUID school, UUID sourceYear, UUID targetYear) {
		Map<UUID, UUID> result = new LinkedHashMap<>();
		jdbc.query("SELECT source_id, target_id FROM " + table + " WHERE establishment_id=:school AND source_academic_year_id=:sourceYear AND target_academic_year_id=:targetYear",
			new MapSqlParameterSource("school", school).addValue("sourceYear", sourceYear).addValue("targetYear", targetYear),
			(rs, row) -> { result.put(rs.getObject("source_id", UUID.class), rs.getObject("target_id", UUID.class)); return 0; });
		return result;
	}
	private MapSqlParameterSource scope(UUID school, UUID sourceYear, UUID targetYear, UUID source, UUID target, String actor) {
		return new MapSqlParameterSource("school", school).addValue("sourceYear", sourceYear).addValue("targetYear", targetYear)
			.addValue("source", source).addValue("target", target).addValue("actor", actor).addValue("now", OffsetDateTime.now());
	}
	private void link(String table, MapSqlParameterSource parameters) {
		jdbc.update("INSERT INTO " + table + " (establishment_id, source_academic_year_id, target_academic_year_id, source_id, target_id, created_by, created_at) "
			+ "VALUES (:school,:sourceYear,:targetYear,:source,:target,:actor,:now)", parameters);
	}
}
