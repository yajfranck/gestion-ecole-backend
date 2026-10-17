package com.galactique.gestionecole.rollover.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.Classroom;

public interface SchoolYearRolloverRepository {
	List<AcademicYear> findYears(UUID school);
	void lockYears(UUID school, UUID sourceYear, UUID targetYear);
	List<SourceClassroom> findClassrooms(UUID school, UUID year);
	Map<UUID, UUID> classLinks(UUID school, UUID sourceYear, UUID targetYear);
	Map<UUID, UUID> classroomLinks(UUID school, UUID sourceYear, UUID targetYear);
	Map<UUID, UUID> feeLinks(UUID school, UUID sourceYear, UUID targetYear);
	void linkClass(UUID school, UUID sourceYear, UUID targetYear, UUID source, UUID target, String actor);
	void linkClassroom(Classroom source, Classroom target, String actor);
	void linkFee(UUID school, UUID sourceYear, UUID targetYear, UUID source, UUID target, String actor);
}
