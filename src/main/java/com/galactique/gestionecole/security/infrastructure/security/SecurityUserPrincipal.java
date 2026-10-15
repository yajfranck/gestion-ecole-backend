package com.galactique.gestionecole.security.infrastructure.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.UserAccount;

public class SecurityUserPrincipal implements UserDetails {
	private final UserAccount account;
	private final Set<GrantedAuthority> authorities;

	public SecurityUserPrincipal(UserAccount account) {
		this.account = account;
		this.authorities = buildAuthorities(account.roles());
	}

	private Set<GrantedAuthority> buildAuthorities(Set<Role> roles) {
		Set<GrantedAuthority> result = new HashSet<>();
		for (Role role : roles) {
			if (!role.enabled()) {
				continue;
			}
			result.add(new SimpleGrantedAuthority("ROLE_" + role.code()));
			role.permissions().forEach(permission -> result.add(new SimpleGrantedAuthority(permission.code())));
		}
		return Set.copyOf(result);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return account.passwordHash();
	}

	@Override
	public String getUsername() {
		return account.username();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !account.locked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return account.enabled();
	}
}