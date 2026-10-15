package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.galactique.gestionecole.security.domain.UserAccountRepository;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:production_flyway_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=validate",
	"spring.flyway.locations=classpath:db/migration",
	"security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@ActiveProfiles("production")
class ProductionFlywayTests {
	@Autowired
	private UserAccountRepository userAccountRepository;

	@Test
	void productionMigrationsDoNotCreateDefaultAdminAccount() {
		assertTrue(userAccountRepository.findByUsername("admin-tech").isEmpty());
	}
}