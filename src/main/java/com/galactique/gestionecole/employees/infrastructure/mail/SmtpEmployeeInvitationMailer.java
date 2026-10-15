package com.galactique.gestionecole.employees.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.galactique.gestionecole.employees.application.EmployeeInvitationMailer;
import com.galactique.gestionecole.employees.domain.Employee;

@Component
public class SmtpEmployeeInvitationMailer implements EmployeeInvitationMailer {
	private static final Logger logger = LoggerFactory.getLogger(SmtpEmployeeInvitationMailer.class);

	private final JavaMailSender mailSender;
	private final String invitationAcceptUrl;
	private final String fromAddress;

	public SmtpEmployeeInvitationMailer(
		JavaMailSender mailSender,
		@Value("${app.employees.invitation-accept-url}") String invitationAcceptUrl,
		@Value("${app.employees.invitation-from}") String fromAddress) {
		this.mailSender = mailSender;
		this.invitationAcceptUrl = invitationAcceptUrl;
		this.fromAddress = fromAddress;
	}

	@Override
	public void sendInvitation(Employee employee, String invitationToken) {
		String invitationUrl = UriComponentsBuilder.fromUriString(invitationAcceptUrl)
			.fragment("token=" + invitationToken)
			.build()
			.toUriString();

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("franckyaj@yahoo.com");
		message.setTo(employee.email());
		message.setSubject("Invitation à rejoindre Gestion Ecole");
		message.setText("Bonjour " + employee.firstName() + ",\n\n"
			+ "Vous êtes invité(e) à rejoindre l'établissement. Ce lien est valable 48 heures et ne peut être utilisé qu'une seule fois :\n"
			+ invitationUrl + "\n\n"
			+ "Après avoir choisi votre mot de passe, connectez-vous sur /api/v1/login.");

		logger.info("Envoi de l’invitation par email pour employeeId={}", employee.id());
		logger.info("sender : " + mailSender.toString());
		try {
			mailSender.send(message);
			logger.info("Invitation par email envoyée pour employeeId={}", employee.id());
		} catch (MailException exception) {
			logger.error("Échec de l’envoi de l’invitation par email pour employeeId={}", employee.id(), exception);
			throw new MailSendException("Échec de l’envoi de l’invitation pour employeeId=" + employee.id(), exception);
		}
	}
}