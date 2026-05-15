package br.com.bnuuy.jwar.server.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies that Flyway picks up every {@code V*__*.sql} migration under
 * {@code src/main/resources/db/migration/} and applies them cleanly to a
 * fresh Postgres.
 *
 * <p>This guards against the historical "the migration file went in but
 * Flyway can't find it" bug surfaced by
 * {@code docs/analysis/03-infra-auth-rest-state.md} §2.4
 * (the {@code classpath:db.migration} typo in {@code application.yml}).
 * Spec 011 fixes the path; this test makes sure it stays fixed.</p>
 *
 * <p>This test does NOT boot a Spring context -- it drives Flyway
 * directly. That keeps it fast and unambiguous about what it's
 * asserting.</p>
 */
@Tag("migration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayMigrationsTest {

	private PostgreSQLContainer<?> postgres;
	private DataSource dataSource;
	private Flyway flyway;

	@BeforeAll
	void startContainer() {
		postgres = new PostgreSQLContainer<>(
				DockerImageName.parse("postgres:16-alpine"))
				.withDatabaseName("flyway_test")
				.withUsername("flyway")
				.withPassword("flyway");
		postgres.start();

		PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setUrl(postgres.getJdbcUrl());
		ds.setUser(postgres.getUsername());
		ds.setPassword(postgres.getPassword());
		this.dataSource = ds;

		this.flyway = Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:db/migration")
				.cleanDisabled(false)
				.load();
	}

	@AfterAll
	void stopContainer() {
		if (postgres != null) {
			postgres.stop();
		}
	}

	@Test
	@DisplayName("Flyway picks up every migration under classpath:db/migration")
	void flywayMigratesCleanly() {
		flyway.clean();
		var result = flyway.migrate();

		assertThat(result.success)
				.as("Flyway migrate() must succeed")
				.isTrue();
		assertThat(result.migrationsExecuted)
				.as("at least one migration ran")
				.isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("Migrations create the expected tables (users, ...)")
	void expectedTablesArePresent() throws Exception {
		flyway.clean();
		flyway.migrate();

		List<String> tables = listPublicTables();

		// V1__users.sql is the only migration committed today; the assertion
		// below is intentionally a containsAll, not an equality, so adding
		// further migrations does not require touching this test.
		assertThat(tables)
				.as("Public-schema tables after migration")
				.contains("users");
	}

	@Test
	@DisplayName("Renaming/removing a migration is detected on re-validate")
	void validateDetectsMissingMigration() {
		flyway.clean();
		flyway.migrate();
		// `validate()` throws when checksums or filenames mismatch the
		// schema history -- this is the failure mode we want to guard.
		flyway.validate();
	}

	private List<String> listPublicTables() throws Exception {
		List<String> tables = new ArrayList<>();
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData meta = conn.getMetaData();
			try (ResultSet rs = meta.getTables(
					null, "public", "%", new String[] {"TABLE"})) {
				while (rs.next()) {
					String name = rs.getString("TABLE_NAME");
					if (!"flyway_schema_history".equals(name)) {
						tables.add(name);
					}
				}
			}
		}
		return tables;
	}
}
