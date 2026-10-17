package com.galactique.gestionecole.enrollments.application;

import com.galactique.gestionecole.enrollments.domain.Enrollment;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import java.util.List;

public interface EnrollmentFeeChargeGateway {
	List<EnrollmentPaymentReceipt> applyFeesAndRecordPayments(Enrollment enrollment,
		List<EnrollmentPaymentInput> payments, EmployeeActor actor);
}
