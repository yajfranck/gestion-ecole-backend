package com.galactique.gestionecole.establishments.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.guardians.domain.Guardian;
import com.galactique.gestionecole.guardians.domain.GuardianRepository;
import com.galactique.gestionecole.security.application.CallerScopeService;
import com.galactique.gestionecole.security.domain.UserAccountRepository;

@Service
public class EstablishmentVisibilityApplicationService {
	private final CallerScopeService callerScope;
	private final UserAccountRepository accounts;
	private final EmployeeRepository employees;
	private final GuardianRepository guardians;
	private final EstablishmentRepository establishments;

	public EstablishmentVisibilityApplicationService(UserAccountRepository accounts, EmployeeRepository employees,
		GuardianRepository guardians,
		EstablishmentRepository establishments, CallerScopeService callerScope) {
		this.callerScope = callerScope;
		this.accounts = accounts;
		this.employees = employees;
		this.guardians = guardians;
		this.establishments = establishments;
	}

	@Transactional(readOnly = true)
	public List<Establishment> findAccessible(String username) {
		var account = accounts.findByUsername(username).filter(a -> a.enabled() && !a.locked())
			.orElseThrow(() -> new AccessDeniedException("Active account required"));
		if (callerScope.hasPlatformScope(username)) {
			return establishments.findAll();
		}

		Set<UUID> accessibleIds = new LinkedHashSet<>();
		for (Employee employee : employees.findAllByUserAccountId(account.id())) {
			if (employee.enabled()) {
				accessibleIds.add(employee.establishmentId());
			}
		}
		guardians.findByUserAccountId(account.id()).filter(Guardian::enabled).ifPresent(guardian ->
			guardians.findChildren(guardian.id()).stream().filter(child -> child.enabled())
				.forEach(child -> accessibleIds.add(child.establishmentId())));

		return accessibleIds.stream().flatMap(id -> establishments.findById(id).stream())
			.filter(Establishment::enabled).toList();
	}

}
