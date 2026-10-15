package com.galactique.gestionecole.security.domain;

import java.util.Optional;

public interface UserAccountRepository {
	Optional<UserAccount> findByUsername(String username);
}