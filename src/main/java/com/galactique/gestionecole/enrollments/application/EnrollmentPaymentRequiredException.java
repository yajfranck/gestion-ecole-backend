package com.galactique.gestionecole.enrollments.application;

import java.util.List;
import java.util.UUID;

public class EnrollmentPaymentRequiredException extends RuntimeException {
	private final List<UUID> feeIds;
	public EnrollmentPaymentRequiredException(List<UUID> feeIds) {
		super("Full payment is required unless an advance is explicitly accepted; an advance must include a positive payment");
		this.feeIds = List.copyOf(feeIds);
	}
	public List<UUID> feeIds() { return feeIds; }
}
