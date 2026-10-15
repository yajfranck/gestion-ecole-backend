package com.galactique.gestionecole.guardians.api.dto;

import com.galactique.gestionecole.guardians.application.GuardianApplicationService.GuardianLinkResult;

public record GuardianLinkResponse(GuardianResponse guardian, String accessStatus) {
	public static GuardianLinkResponse from(GuardianLinkResult result) {
		return new GuardianLinkResponse(GuardianResponse.from(result.guardian()), result.status());
	}
}