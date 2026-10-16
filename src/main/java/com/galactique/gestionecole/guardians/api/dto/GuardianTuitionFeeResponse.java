package com.galactique.gestionecole.guardians.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.tuition.application.TuitionApplicationService.ChargeBalance;

public record GuardianTuitionFeeResponse(UUID chargeId, UUID feeId, UUID academicYearId, String name,
	BigDecimal amount, String currency, LocalDate dueDate, BigDecimal totalPaid, BigDecimal balance,
	boolean overdue) {
	public static GuardianTuitionFeeResponse from(ChargeBalance chargeBalance) {
		var charge = chargeBalance.charge();
		return new GuardianTuitionFeeResponse(charge.id(), charge.feeId(), charge.academicYearId(),
			charge.name(), charge.amount(), charge.currency(), charge.dueDate(), chargeBalance.paid(),
			chargeBalance.balance(), chargeBalance.overdue());
	}
}
