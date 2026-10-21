package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

@SuppressWarnings({"java:S101", "java:S2077"})
public class V16__AllowMultiEstablishmentEmployees extends BaseJavaMigration {
	private static final Set<String> UNIQUE_COLUMNS = Set.of("employee_number", "email", "user_account_id");
	private static final Pattern SAFE_IDENTIFIER = Pattern.compile("\\w+");

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		Map<String, Set<String>> constraints = findUniqueConstraints(connection);
		try (Statement statement = connection.createStatement()) {
			statement.execute("ALTER TABLE employees DROP CONSTRAINT fk_employees_user_account");
			for (Map.Entry<String, Set<String>> constraint : constraints.entrySet()) {
				if (constraint.getValue().size() == 1
					&& UNIQUE_COLUMNS.contains(constraint.getValue().iterator().next())) {
					String constraintName = constraint.getKey();
					if (!SAFE_IDENTIFIER.matcher(constraintName).matches()) {
						throw new IllegalStateException("Unexpected unique constraint identifier");
					}
					statement.execute("ALTER TABLE employees DROP CONSTRAINT \"" + constraintName + "\"");
				}
			}
			if (connection.getMetaData().getDatabaseProductName().startsWith("H2")) {
				dropH2LegacyUniqueIndexes(connection, statement);
			}
			statement.execute("ALTER TABLE employees ADD CONSTRAINT fk_employees_user_account "
				+ "FOREIGN KEY (user_account_id) REFERENCES security_user_accounts (id) ON DELETE SET NULL");
			statement.execute("ALTER TABLE employees ADD CONSTRAINT uq_employees_establishment_employee_number "
				+ "UNIQUE (establishment_id, employee_number)");
			statement.execute("ALTER TABLE employees ADD CONSTRAINT uq_employees_establishment_email "
				+ "UNIQUE (establishment_id, email)");
		}
		Map<String, Set<String>> remainingConstraints = findUniqueConstraints(connection);
		boolean globalUniqueRemains = remainingConstraints.values().stream().anyMatch(columns ->
			columns.size() == 1 && UNIQUE_COLUMNS.contains(columns.iterator().next()));
		if (globalUniqueRemains) {
			throw new IllegalStateException("Global employee uniqueness constraints remain after migration");
		}
	}

	private void dropH2LegacyUniqueIndexes(Connection connection, Statement statement) throws Exception {
		String query = "SELECT index_name FROM information_schema.index_columns "
			+ "WHERE LOWER(table_name) = LOWER(?) AND LOWER(table_schema) = LOWER(?) AND is_unique = TRUE "
			+ "GROUP BY index_name HAVING COUNT(*) = 1 "
			+ "AND LOWER(MIN(column_name)) IN ('employee_number', 'email', 'user_account_id')";
		try (PreparedStatement metadataQuery = connection.prepareStatement(query)) {
			metadataQuery.setString(1, "employees");
			metadataQuery.setString(2, connection.getSchema());
			try (ResultSet indexes = metadataQuery.executeQuery()) {
				while (indexes.next()) {
					String indexName = indexes.getString("index_name");
					if (!SAFE_IDENTIFIER.matcher(indexName).matches()) {
						throw new IllegalStateException("Unexpected employee index identifier");
					}
					statement.execute("DROP INDEX \"" + indexName + "\"");
				}
			}
		}
	}

	private Map<String, Set<String>> findUniqueConstraints(Connection connection) throws Exception {
		Map<String, Set<String>> constraints = new LinkedHashMap<>();
		String query = "SELECT tc.constraint_name, kcu.column_name "
			+ "FROM information_schema.table_constraints tc "
			+ "JOIN information_schema.key_column_usage kcu "
			+ "ON tc.constraint_catalog = kcu.constraint_catalog "
			+ "AND tc.constraint_schema = kcu.constraint_schema "
			+ "AND tc.constraint_name = kcu.constraint_name "
			+ "AND tc.table_schema = kcu.table_schema "
			+ "AND tc.table_name = kcu.table_name "
			+ "WHERE LOWER(tc.table_name) = LOWER(?) AND LOWER(tc.table_schema) = LOWER(?) "
			+ "AND tc.constraint_type = 'UNIQUE'";
		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setString(1, "employees");
			statement.setString(2, connection.getSchema());
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					constraints.computeIfAbsent(result.getString("constraint_name"), ignored -> new LinkedHashSet<>())
						.add(result.getString("column_name").toLowerCase(java.util.Locale.ROOT));
				}
			}
		}
		return constraints;
	}
}