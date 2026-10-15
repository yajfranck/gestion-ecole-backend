package com.galactique.gestionecole.guardians.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.galactique.gestionecole.guardians.application.GuardianInvitationMailer;
import com.galactique.gestionecole.guardians.domain.Guardian;

@Component
public class SmtpGuardianInvitationMailer implements GuardianInvitationMailer {
	private final JavaMailSender mailSender;
	private final String invitationAcceptUrl;
	private final String fromAddress;

	public SmtpGuardianInvitationMailer(JavaMailSender mailSender,
		@Value("${app.guardians.invitation-accept-url}") String invitationAcceptUrl,
		@Value("${app.guardians.invitation-from}") String fromAddress) {
		this.mailSender = mailSender;
		this.invitationAcceptUrl = invitationAcceptUrl;
		this.fromAddress = fromAddress;
	}

	@Override
	public void sendInvitation(Guardian guardian, String invitationToken) {
		String invitationUrl = UriComponentsBuilder.fromUriString(invitationAcceptUrl)
			.fragment("token=" + invitationToken).build().toUriString();
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(guardian.email());
		message.setSubject("Invitation à accéder à l'espace famille Gestion Ecole");
		message.setText("Bonjour " + guardian.firstName() + ",\n\n"
			+ "Créez votre accès parent avec ce lien à usage unique, valable 48 heures :\n"
			+ invitationUrl + "\n\nConnectez-vous ensuite avec votre adresse e-mail.");
		mailSender.send(message);
	}
}