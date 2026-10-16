package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.enrollments.application.EnrollmentFeeChargeGateway;
import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.tuition.domain.TuitionFee;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;

@Component
public class JpaEnrollmentFeeChargeGateway implements EnrollmentFeeChargeGateway {
	private final TuitionRepository tuitionRepository;
	private final StudentRepository studentRepository;
	private final SchoolClassRepository schoolClassRepository;

	public JpaEnrollmentFeeChargeGateway(TuitionRepository tuitionRepository,
		StudentRepository studentRepository, SchoolClassRepository schoolClassRepository) {
		this.tuitionRepository = tuitionRepository;
		this.studentRepository = studentRepository;
		this.schoolClassRepository = schoolClassRepository;
	}

	@Override
	@Transactional
	public void applyExistingFees(Enrollment enrollment) {
		var student = studentRepository.findByIdAndEstablishmentId(
			enrollment.studentId(), enrollment.establishmentId()).orElseThrow();
		var schoolClass = schoolClassRepository.findByIdAndEstablishmentIdAndAcademicYearId(
			enrollment.schoolClassId(), enrollment.establishmentId(), enrollment.academicYearId()).orElseThrow();
		for (TuitionFee fee : tuitionRepository.findFees(enrollment.establishmentId(), enrollment.academicYearId())) {
			if (fee.enabled()
				&& (fee.studentId() == null || fee.studentId().equals(enrollment.studentId()))
				&& (fee.schoolClassId() == null || fee.schoolClassId().equals(schoolClass.id()))
				&& !tuitionRepository.existsCharge(fee.id(), enrollment.studentId())) {
				tuitionRepository.saveCharge(fee, student);
			}
		}
	}
}