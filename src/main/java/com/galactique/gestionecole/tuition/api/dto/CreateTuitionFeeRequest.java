package com.galactique.gestionecole.tuition.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTuitionFeeRequest(
	@NotBlank @Size(max = 120) String name,
	@NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
	@NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
	@NotNull LocalDate dueDate,
	@NotNull UUID feeTypeId,
	UUID schoolClassId,
	UUID studentId,
	@Size(min = 1) List<@NotNull UUID> schoolClassIds) {
}
