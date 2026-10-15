package com.galactique.gestionecole.guardians.domain;

import java.util.Optional;

public interface GuardianInvitationRepository {
	GuardianInvitation save(GuardianInvitation invitation);
	Optional<GuardianInvitation> findByTokenHashForUpdate(String tokenHash);
	void markAccepted(GuardianInvitation invitation);
}