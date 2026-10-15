package com.galactique.gestionecole.security.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.galactique.gestionecole.security.domain.UserAccountRepository;

@Service
public class SchoolUserDetailsService implements UserDetailsService {
	private final UserAccountRepository userAccountRepository;

	public SchoolUserDetailsService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userAccountRepository.findByUsername(username)
			.map(SecurityUserPrincipal::new)
			.orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
	}
}