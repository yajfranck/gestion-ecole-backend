package com.galactique.gestionecole.tuition.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.tuition.application.TuitionApplicationService.ChargeBalance;

public record StudentFeeChargeResponse(UUID id, UUID feeId, UUID establishmentId,
	UUID academicYearId, UUID studentId, String studentNumber, String studentFirstName,
	String studentLastName, String name, BigDecimal amount, String currency,
	LocalDate dueDate, BigDecimal totalPaid, BigDecimal balance, boolean overdue,
	List<TuitionPaymentResponse> payments) {
	public static StudentFeeChargeResponse from(ChargeBalance balance) {
		var charge = balance.charge();
		return new StudentFeeChargeResponse(charge.id(), charge.feeId(), charge.establishmentId(),
			charge.academicYearId(), charge.studentId(), charge.studentNumber(), charge.studentFirstName(),
			charge.studentLastName(), charge.name(), charge.amount(), charge.currency(),
			charge.dueDate(), balance.paid(), balance.balance(), balance.overdue(),
			balance.payments().stream().map(TuitionPaymentResponse::from).toList());
	}
}