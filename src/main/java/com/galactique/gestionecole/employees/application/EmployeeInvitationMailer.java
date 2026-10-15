package com.galactique.gestionecole.employees.application;

import com.galactique.gestionecole.employees.domain.Employee;

public interface EmployeeInvitationMailer {
	void sendInvitation(Employee employee, String invitationToken);
}