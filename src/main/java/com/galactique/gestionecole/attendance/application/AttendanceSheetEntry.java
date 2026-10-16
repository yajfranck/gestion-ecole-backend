package com.galactique.gestionecole.attendance.application;

import com.galactique.gestionecole.attendance.domain.AttendanceRecord;
import com.galactique.gestionecole.students.domain.Student;

public record AttendanceSheetEntry(Student student, AttendanceRecord attendanceRecord) {
}