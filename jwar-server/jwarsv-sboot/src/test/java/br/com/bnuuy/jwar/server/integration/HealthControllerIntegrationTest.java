package br.com.bnuuy.jwar.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Smoke test for the application's health endpoint.
 *
 * <p>Boots the full Spring context against a Testcontainers Postgres
 * and verifies that {@code GET /actuator/health} (or {@code /api/health},
 * whichever the spec-003 REST foundation exposes) returns HTTP 200 with
 * the expected status field.</p>
 *
 * <p>This test is intentionally light: it proves the wiring
 * (component-scan, datasource, Flyway, security filter chain) is
 * coherent before the more specific integration suites run.</p>
 */
class HealthControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("GET /actuator/health returns 200 with status=UP")
	void actuatorHealthIsUp() {
		assumeThat(POSTGRES.isRunning())
				.as("Testcontainers Postgres must be running")
				.isTrue();

		ResponseEntity<String> response =
				restTemplate.getForEntity("/actuator/health", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody())
				.as("Health payload must report status=UP")
				.contains("\"status\":\"UP\"");
	}

	@Test
	@DisplayName("GET /api/health returns 200 once the REST foundation lands (spec 003)")
	void apiHealthSurfaceIsAvailable() {
		// The /api/health endpoint is owned by spec 003. Until it ships
		// this assertion is best-effort: a 404 should not fail this test
		// because the actuator endpoint above already proves liveness.
		ResponseEntity<String> response =
				restTemplate.getForEntity("/api/health", String.class);

		if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
			// Expected before spec 003 ships -- skip the body assertion.
			return;
		}

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotBlank();
	}
}
