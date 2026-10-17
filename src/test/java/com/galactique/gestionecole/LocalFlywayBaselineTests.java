package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:local_baseline_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("local")
class LocalFlywayBaselineTests {
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void initializesOnlyLocalAdminWithoutBusinessFixtures() {
        assertEquals(10, flyway.info().applied().length);
        assertEquals("10", flyway.info().current().getVersion().toString());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM security_user_accounts", Integer.class));
        assertEquals("admin-tech", jdbc.queryForObject("SELECT username FROM security_user_accounts", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM security_user_roles ur JOIN security_roles r ON r.id = ur.role_id WHERE r.code = 'ADMIN'", Integer.class));
        for (String table : java.util.List.of("establishments", "students", "platform_licenses")) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class), table);
        }
    }
}
