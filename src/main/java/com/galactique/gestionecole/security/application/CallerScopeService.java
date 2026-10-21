package com.galactique.gestionecole.security.application;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.security.domain.UserAccount;
import com.galactique.gestionecole.security.domain.UserAccountRepository;

/** Data scope only. Functional permissions belong to controller method annotations. */
@Service
@Transactional(readOnly = true)
public class CallerScopeService {
	private final UserAccountRepository accounts;
	private final EmployeeRepository employees;
	private final EstablishmentRepository establishments;
	private final JdbcTemplate jdbc;

	public CallerScopeService(UserAccountRepository accounts, EmployeeRepository employees,
		EstablishmentRepository establishments, JdbcTemplate jdbc) {
		this.accounts = accounts;
		this.employees = employees;
		this.establishments = establishments;
		this.jdbc = jdbc;
	}

	public UserAccount requireAccount(String username) {
		return accounts.findByUsername(username).filter(account -> account.enabled() && !account.locked())
			.orElseThrow(() -> new AccessDeniedException("Active account required"));
	}

	public boolean hasPlatformScope(String username) {
		UserAccount account = requireAccount(username);
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT COUNT(*) > 0 FROM security_user_roles ur
			JOIN security_roles r ON r.id = ur.role_id AND r.enabled = TRUE
			JOIN security_role_scopes s ON s.role_id = r.id
			WHERE ur.user_account_id = ? AND s.scope_code = 'PLATFORM'
			""", Boolean.class, account.id()));
	}

	public boolean isPlatformRole(String roleCode) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT COUNT(*) > 0 FROM security_role_scopes s
			JOIN security_roles r ON r.id = s.role_id
			WHERE r.code = ? AND s.scope_code = 'PLATFORM'
			""", Boolean.class, roleCode));
	}

	public void requirePlatformScope(String username) {
		if (!hasPlatformScope(username)) {
			throw new AccessDeniedException("Platform data scope required");
		}
	}

	public Employee requireMember(String username, UUID establishmentId) {
		UserAccount account = requireAccount(username);
		return employees.findByUserAccountIdAndEstablishmentId(account.id(), establishmentId)
			.filter(Employee::enabled).filter(employee -> account.id().equals(employee.userAccountId()))
			.orElseThrow(() -> new AccessDeniedException("Establishment is outside caller scope"));
	}

	public boolean hasEstablishmentAccess(String username, UUID establishmentId) {
		if (username == null || establishmentId == null
			|| establishments.findById(establishmentId).filter(Establishment::enabled).isEmpty()) {
			return false;
		}
		UserAccount account = accounts.findByUsername(username).filter(user -> user.enabled() && !user.locked())
			.orElse(null);
		if (account == null) {
			return false;
		}
		return hasPlatformScope(username) || employees.findByUserAccountIdAndEstablishmentId(account.id(), establishmentId)
			.filter(employee -> employee.enabled())
			.filter(employee -> account.id().equals(employee.userAccountId()))
			.isPresent();
	}

	public void requireEstablishment(String username, UUID establishmentId) {
		if (!hasPlatformScope(username)) {
			requireMember(username, establishmentId);
		}
	}

	public List<Establishment> findStaffEstablishments(String username) {
		UserAccount account = requireAccount(username);
		if (hasPlatformScope(username)) {
			return establishments.findAll();
		}
		return employees.findAllByUserAccountId(account.id()).stream().filter(Employee::enabled)
			.map(Employee::establishmentId).distinct()
			.flatMap(id -> establishments.findById(id).stream()).filter(Establishment::enabled).toList();
	}
}
