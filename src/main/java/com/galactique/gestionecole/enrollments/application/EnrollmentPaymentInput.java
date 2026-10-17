package com.galactique.gestionecole.enrollments.application;

import java.math.BigDecimal;
import java.util.UUID;

public record EnrollmentPaymentInput(UUID feeId, BigDecimal amount, String paymentMethodCode, String reference) { }
