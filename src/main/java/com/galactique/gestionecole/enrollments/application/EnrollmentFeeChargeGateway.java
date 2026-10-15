package com.galactique.gestionecole.enrollments.application;

import com.galactique.gestionecole.enrollments.domain.Enrollment;

public interface EnrollmentFeeChargeGateway {
	void applyExistingFees(Enrollment enrollment);
}