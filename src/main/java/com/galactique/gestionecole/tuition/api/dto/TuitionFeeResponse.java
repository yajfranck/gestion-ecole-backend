package com.galactique.gestionecole.tuition.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import com.galactique.gestionecole.tuition.domain.TuitionFee;

public record TuitionFeeResponse(UUID id, UUID establishmentId, UUID academicYearId, String name,
	BigDecimal amount, String currency, LocalDate dueDate, String scope, UUID feeTypeId,
	String feeTypeCode, String feeTypeName, UUID schoolClassId,
	UUID studentId, Instant createdAt, List<UUID> schoolClassIds) {
	public static TuitionFeeResponse from(TuitionFee fee) {
		String scope = "ACADEMIC_YEAR";
		if (fee.studentId() != null) {
			scope = "STUDENT";
		} else if (!fee.schoolClassIds().isEmpty()) {
			scope = "SCHOOL_CLASS";
		}
		return new TuitionFeeResponse(fee.id(), fee.establishmentId(), fee.academicYearId(), fee.name(),
			fee.amount(), fee.currency(), fee.dueDate(), scope, fee.feeTypeId(), fee.feeTypeCode(), fee.feeTypeName(),
			fee.schoolClassId(), fee.studentId(), fee.createdAt(), fee.schoolClassIds());
	}
}
