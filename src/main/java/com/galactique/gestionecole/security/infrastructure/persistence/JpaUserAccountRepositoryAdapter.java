package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.security.domain.UserAccount;
import com.galactique.gestionecole.security.domain.UserAccountRepository;

@Repository
public class JpaUserAccountRepositoryAdapter implements UserAccountRepository {
	private final UserAccountJpaRepository userAccountJpaRepository;
	private final SecurityPersistenceMapper mapper;

	public JpaUserAccountRepositoryAdapter(
		UserAccountJpaRepository userAccountJpaRepository,
		SecurityPersistenceMapper mapper) {
		this.userAccountJpaRepository = userAccountJpaRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserAccount> findByUsername(String username) {
		return userAccountJpaRepository.findByUsernameIgnoreCase(username.strip().toLowerCase(Locale.ROOT))
			.map(mapper::toDomain);
	}
}