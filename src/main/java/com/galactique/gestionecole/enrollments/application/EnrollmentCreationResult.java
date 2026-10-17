package com.galactique.gestionecole.enrollments.application;

import java.util.List;
import com.galactique.gestionecole.enrollments.domain.Enrollment;

public record EnrollmentCreationResult(Enrollment enrollment, List<EnrollmentPaymentReceipt> payments) {
	public EnrollmentCreationResult { payments = List.copyOf(payments); }
}
