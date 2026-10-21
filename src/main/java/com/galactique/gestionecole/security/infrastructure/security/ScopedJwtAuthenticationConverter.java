package com.galactique.gestionecole.security.infrastructure.security;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.security.application.CallerScopeService;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.domain.UserAccount;

/** Resolves current authorities in the requested tenant before method authorization. */
@Component
public class ScopedJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
	private static final Pattern ESTABLISHMENT_PATH = Pattern.compile("/establishments/([^/;]+)(?:;[^/]*)?(?:/|$)");
	private final CallerScopeService scope;
	private final EmployeeRepository employees;
	private final SecurityDirectoryRepository directory;
	private final PositionRepository positions;

	public ScopedJwtAuthenticationConverter(CallerScopeService scope, EmployeeRepository employees,
		SecurityDirectoryRepository directory, PositionRepository positions) {
		this.positions = positions;
		this.scope = scope;
		this.employees = employees;
		this.directory = directory;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UserAccount account;
		try { account = scope.requireAccount(jwt.getSubject()); }
		catch (org.springframework.security.access.AccessDeniedException exception) {
			throw new OAuth2AuthenticationException("invalid_token");
		}
		UUID establishmentId = requestedEstablishment();
		java.util.Map<String, Set<UUID>> activeMembershipsByRole = new java.util.HashMap<>();
		Set<String> allMemberRoles = new HashSet<>();
		for (Employee employee : employees.findAllByUserAccountId(account.id())) {
			allMemberRoles.addAll(employee.directRoleCodes());
			positions.findByIdAndEstablishmentId(employee.positionId(), employee.establishmentId())
				.ifPresent(position -> allMemberRoles.addAll(position.roleCodes()));
			if (employee.enabled() && (establishmentId == null || employee.establishmentId().equals(establishmentId))) {
				for (String roleCode : employee.roleCodes()) {
					activeMembershipsByRole.computeIfAbsent(roleCode, ignored -> new HashSet<>()).add(employee.establishmentId());
				}
			}
		}
		Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		var roles = establishmentId == null ? account.roles() : directory.findRolesForEstablishment(establishmentId);
		Set<String> accountRoleCodes = account.roles().stream().map(Role::code).collect(java.util.stream.Collectors.toSet());
		for (Role role : roles) {
			if (!role.enabled()) { continue; }
			Set<UUID> memberSchools = activeMembershipsByRole.getOrDefault(role.code(), Set.of());
			boolean memberRole = accountRoleCodes.contains(role.code()) && !memberSchools.isEmpty()
				&& (role.establishmentId() == null || memberSchools.contains(role.establishmentId()));
			boolean globalRole = role.establishmentId() == null && accountRoleCodes.contains(role.code())
				&& !allMemberRoles.contains(role.code());
			if (!memberRole && !globalRole) { continue; }
			role.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.code())));
		}
		return new JwtAuthenticationToken(jwt, authorities, account.username());
	}

	private UUID requestedEstablishment() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			var matcher = ESTABLISHMENT_PATH.matcher(attributes.getRequest().getRequestURI());
			if (matcher.find()) {
				try { return UUID.fromString(matcher.group(1)); }
				catch (IllegalArgumentException exception) { throw new OAuth2AuthenticationException("invalid_token"); }
			}
			return null;
		}
		throw new OAuth2AuthenticationException("invalid_token");
	}
}
