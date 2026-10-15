package com.galactique.gestionecole.employees.domain;

import java.util.Optional;

public interface EmployeeInvitationRepository {
	EmployeeInvitation save(EmployeeInvitation invitation);

	Optional<EmployeeInvitation> findByTokenHashForUpdate(String tokenHash);

	void markAccepted(EmployeeInvitation invitation);
}