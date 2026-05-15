package br.com.bnuuy.jwar.server.integration;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test-scope configuration that replaces the production {@link FirebaseAuth}
 * bean with a deterministic stub.
 *
 * <p>Tokens of the form {@code test:<uid>} resolve to the matching UID
 * (with a synthesized email {@code <uid>@test.local}). Anything else
 * throws an {@link IllegalArgumentException}, which the auth filter is
 * expected to translate to a 401.</p>
 *
 * <p>Import into a concrete integration test with
 * {@code @Import(FirebaseTestConfig.class)}.</p>
 */
@TestConfiguration
public class FirebaseTestConfig {

	/**
	 * Mockito mock for {@link FirebaseAuth}. Wired with a stub that
	 * decodes deterministic {@code test:<uid>} tokens.
	 */
	@Bean
	@Primary
	public FirebaseAuth firebaseAuth() throws Exception {
		FirebaseAuth mock = Mockito.mock(FirebaseAuth.class);

		Mockito.when(mock.verifyIdToken(Mockito.anyString()))
				.thenAnswer(invocation -> {
					String token = invocation.getArgument(0, String.class);
					if (token == null || !token.startsWith("test:")) {
						throw new IllegalArgumentException(
								"Test token must start with 'test:' -- got: " + token);
					}
					String uid = token.substring("test:".length());
					return stubFirebaseToken(uid);
				});

		return mock;
	}

	/**
	 * {@link FirebaseToken} is a final class with package-private
	 * construction, so we mock it too. Tests that assert on the token
	 * claim shape should adjust this stub as needed.
	 */
	private static FirebaseToken stubFirebaseToken(String uid) {
		FirebaseToken token = Mockito.mock(FirebaseToken.class);
		Mockito.when(token.getUid()).thenReturn(uid);
		Mockito.when(token.getEmail()).thenReturn(uid + "@test.local");
		Mockito.when(token.getName()).thenReturn("Test " + uid);
		Mockito.when(token.isEmailVerified()).thenReturn(true);
		return token;
	}
}
