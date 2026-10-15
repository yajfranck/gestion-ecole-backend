package com.galactique.gestionecole.guardians.application;

import com.galactique.gestionecole.guardians.domain.Guardian;

public interface GuardianInvitationMailer {
	void sendInvitation(Guardian guardian, String invitationToken);
}