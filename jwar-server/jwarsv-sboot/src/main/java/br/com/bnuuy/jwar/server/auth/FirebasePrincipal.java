package br.com.bnuuy.jwar.server.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Authenticated user context attached to the Spring SecurityContext.
 */
@Getter
@AllArgsConstructor
@ToString
public class FirebasePrincipal {
    private final UUID userId;
    private final String firebaseUid;
    private final String email;
    private final String displayName;
    private final String provider;
    private final String rawIdToken;

    public String getName() {
        return userId != null ? userId.toString() : firebaseUid;
    }
}
