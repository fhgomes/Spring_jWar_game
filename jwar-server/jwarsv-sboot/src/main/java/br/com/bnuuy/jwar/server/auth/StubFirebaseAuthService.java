package br.com.bnuuy.jwar.server.auth;

import br.com.bnuuy.jwar.server.exception.ConflictException;
import br.com.bnuuy.jwar.server.exception.UnauthorizedException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev/test stand-in for the Firebase Admin SDK.
 *
 * <p>Token format: {@code dev:<uid>} or {@code dev:<uid>:<email>}. Tokens issued by
 * {@link #createCustomToken(String)} are accepted as-is on subsequent verify calls.
 */
@Slf4j
public class StubFirebaseAuthService implements FirebaseAuthService {

    private final ConcurrentMap<String, CreatedUser> usersByEmail = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CreatedUser> usersByUid = new ConcurrentHashMap<>();

    @Override
    public VerifiedToken verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Token de autenticação ausente", "UNAUTHENTICATED");
        }
        String trimmed = idToken.trim();
        if (!trimmed.startsWith("dev:")) {
            throw new UnauthorizedException("Token inválido", "INVALID_TOKEN");
        }
        String[] parts = trimmed.split(":", 3);
        if (parts.length < 2 || parts[1].isBlank()) {
            throw new UnauthorizedException("Token inválido", "INVALID_TOKEN");
        }
        String uid = parts[1];
        String email = parts.length >= 3 && !parts[2].isBlank() ? parts[2] : uid + "@dev.local";
        CreatedUser known = usersByUid.get(uid);
        String displayName = known != null && known.displayName() != null ? known.displayName() : uid;
        return new VerifiedToken(uid, email, true, displayName, "password");
    }

    @Override
    public CreatedUser createUser(String email, String password, String displayName) {
        if (usersByEmail.containsKey(email.toLowerCase())) {
            throw new ConflictException("E-mail já cadastrado", "EMAIL_ALREADY_IN_USE");
        }
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        CreatedUser user = new CreatedUser(uid, email, displayName);
        usersByEmail.put(email.toLowerCase(), user);
        usersByUid.put(uid, user);
        log.info("[stub] Created dev user uid={} email={}", uid, email);
        return user;
    }

    @Override
    public String createCustomToken(String uid) {
        CreatedUser user = usersByUid.get(uid);
        String email = user != null ? user.email() : uid + "@dev.local";
        return "dev:" + uid + ":" + email;
    }

    @Override
    public void updateUser(String uid, String displayName, String photoUrl) {
        CreatedUser existing = usersByUid.get(uid);
        if (existing != null && displayName != null) {
            CreatedUser updated = new CreatedUser(uid, existing.email(), displayName);
            usersByUid.put(uid, updated);
            usersByEmail.put(existing.email().toLowerCase(), updated);
        }
    }

    @Override
    public void deleteUser(String uid) {
        CreatedUser removed = usersByUid.remove(uid);
        if (removed != null) {
            usersByEmail.remove(removed.email().toLowerCase());
        }
    }
}
