package com.galactique.gestionecole.tuition.api.dto;

import java.util.UUID;

import com.galactique.gestionecole.tuition.domain.TuitionPaymentMethod;

public record TuitionPaymentMethodResponse(UUID id, UUID establishmentId, String code, String name) {
	public static TuitionPaymentMethodResponse from(TuitionPaymentMethod method) {
		return new TuitionPaymentMethodResponse(method.id(), method.establishmentId(), method.code(), method.name());
	}
}