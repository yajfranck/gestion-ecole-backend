package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

public interface ClassroomStatisticsProjection {
	Object getSchoolClassId();
	Long getClassroomCount();
	Long getTotalCapacity();
	Long getEnrolledStudentCount();
}