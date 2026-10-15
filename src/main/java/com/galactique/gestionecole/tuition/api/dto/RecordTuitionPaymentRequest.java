package com.galactique.gestionecole.tuition.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordTuitionPaymentRequest(
	@NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
	@NotBlank @Size(max = 40) String paymentMethodCode,
	@Size(max = 120) String reference) {
}