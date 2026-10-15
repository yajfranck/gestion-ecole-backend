package com.galactique.gestionecole.tuition.api.dto;

import com.galactique.gestionecole.tuition.application.TuitionApplicationService.FeeCreationResult;

public record FeeCreationResponse(TuitionFeeResponse fee, int chargesCreated) {
	public static FeeCreationResponse from(FeeCreationResult result) {
		return new FeeCreationResponse(TuitionFeeResponse.from(result.fee()), result.chargesCreated());
	}
}