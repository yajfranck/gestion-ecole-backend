package com.galactique.gestionecole.schoolclasses.domain;

public record ClassroomStatistics(long classroomCount, long totalCapacity, long enrolledStudentCount) {
	public static ClassroomStatistics empty() {
		return new ClassroomStatistics(0, 0, 0);
	}
}