package br.com.bnuuy.jwar.server.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for Spring Boot integration tests.
 *
 * <p>Boots the full application context against a shared {@link PostgreSQLContainer}.
 * The container is declared {@code static} so it survives across test
 * classes within a single JVM fork -- Testcontainers' default Ryuk
 * reaper will dispose of it on exit.</p>
 *
 * <p>To enable per-class isolation, either annotate the concrete subclass
 * with {@link org.springframework.test.annotation.DirtiesContext} or
 * truncate tables in {@code @BeforeEach}.</p>
 *
 * <p><b>Skip strategy:</b> {@code @Tag("integration")} lets the build
 * filter these out when Docker is not available
 * (see {@code FR-edge-case} in spec 012 -- "Testcontainers cannot pull
 * the Postgres image").</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
public abstract class AbstractIntegrationTest {

	@SuppressWarnings("resource") // Ryuk will reap the container at JVM exit.
	protected static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
					.withDatabaseName("jwar_test")
					.withUsername("jwar_test")
					.withPassword("jwar_test_pw")
					.withReuse(true);

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void registerDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name",
				() -> "org.postgresql.Driver");

		// Make sure Flyway uses the same datasource and points at the
		// correct (fixed) location.
		registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
		registry.add("spring.flyway.user", POSTGRES::getUsername);
		registry.add("spring.flyway.password", POSTGRES::getPassword);
		registry.add("spring.flyway.locations", () -> "classpath:db/migration");
	}
}
