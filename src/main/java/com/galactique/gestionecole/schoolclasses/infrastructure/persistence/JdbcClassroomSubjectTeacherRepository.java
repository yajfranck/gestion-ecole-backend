package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.ClassroomSubjectTeacher;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomSubjectTeacherRepository;

@Repository
public class JdbcClassroomSubjectTeacherRepository implements ClassroomSubjectTeacherRepository {
	private final JdbcTemplate jdbc;

	public JdbcClassroomSubjectTeacherRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, List<ClassroomSubjectTeacher>> findAllByClass(UUID establishmentId, UUID schoolClassId) {
		Map<UUID, List<ClassroomSubjectTeacher>> result = new LinkedHashMap<>();
		jdbc.query("""
			SELECT t.subject_id, t.classroom_id, t.employee_id
			FROM classroom_subject_teachers t
			JOIN classrooms c ON c.id = t.classroom_id AND c.enabled = TRUE
			JOIN employees e ON e.id = t.employee_id AND e.enabled = TRUE
			JOIN school_class_subjects s ON s.school_class_id = t.school_class_id
				AND s.subject_id = t.subject_id AND s.enabled = TRUE
			WHERE t.establishment_id = ? AND t.school_class_id = ?
			ORDER BY t.subject_id, c.name, c.id
			""", row -> {
				UUID subjectId = row.getObject("subject_id", UUID.class);
				result.computeIfAbsent(subjectId, key -> new ArrayList<>()).add(new ClassroomSubjectTeacher(
					row.getObject("classroom_id", UUID.class), row.getObject("employee_id", UUID.class)));
			}, establishmentId, schoolClassId);
		return result;
	}

	@Override
	@Transactional
	public void replace(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID subjectId,
		List<ClassroomSubjectTeacher> teachers) {
		jdbc.update("DELETE FROM classroom_subject_teachers WHERE establishment_id = ? AND school_class_id = ? AND subject_id = ?",
			establishmentId, schoolClassId, subjectId);
		jdbc.batchUpdate("""
			INSERT INTO classroom_subject_teachers
				(establishment_id, academic_year_id, school_class_id, subject_id, classroom_id, employee_id)
			VALUES (?, ?, ?, ?, ?, ?)
			""", teachers, 100, (statement, teacher) -> {
				statement.setObject(1, establishmentId);
				statement.setObject(2, academicYearId);
				statement.setObject(3, schoolClassId);
				statement.setObject(4, subjectId);
				statement.setObject(5, teacher.classroomId());
				statement.setObject(6, teacher.employeeId());
			});
	}
}
