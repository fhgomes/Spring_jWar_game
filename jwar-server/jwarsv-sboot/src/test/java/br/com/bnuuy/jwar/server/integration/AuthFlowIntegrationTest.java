package br.com.bnuuy.jwar.server.integration;

import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;

/**
 * Auth-flow integration test.
 *
 * <p>Asserts that:</p>
 * <ol>
 *   <li>A deterministic {@code test:<uid>} bearer token (issued by the
 *       Mockito {@code FirebaseAuth} stub from
 *       {@link FirebaseTestConfig}) is accepted by the security filter.</li>
 *   <li>The first request for a brand-new UID provisions a row in the
 *       {@code users} table (just-in-time user creation).</li>
 *   <li>{@code GET /api/me} returns the persisted user's profile.</li>
 * </ol>
 *
 * <p>Disabled until spec 004 (auth + users) and spec 003 (REST
 * foundation) ship; the controllers and the persistence layer this
 * test exercises do not exist yet.</p>
 */
@Import(FirebaseTestConfig.class)
@Disabled("Enable when spec 004 (auth + users) wires AuthFilter + /api/me")
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("First call with a fresh test:<uid> token creates a User row")
	void firstCallProvisionsUser() {
		assumeThat(POSTGRES.isRunning()).isTrue();

		// TODO(spec 004): once /api/me + the JIT user-provisioning
		// machinery exist:
		//   1. GET /api/me with Authorization: Bearer test:e2e-uid-001
		//   2. assert 200 + body { uid: "e2e-uid-001", ... }
		//   3. query the `users` table and assert the row is present
		//      with the expected email/displayName derived from the
		//      stubbed FirebaseToken (see FirebaseTestConfig).
		//
		// Implementation hint: use TestRestTemplate#exchange and set the
		// Authorization header on an HttpEntity<Void>.
	}
}
