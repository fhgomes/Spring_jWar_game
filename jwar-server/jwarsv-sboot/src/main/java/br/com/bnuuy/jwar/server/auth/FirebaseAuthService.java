package br.com.bnuuy.jwar.server.auth;

/**
 * Abstraction around the Firebase Admin SDK so dev/test profiles can swap in a stub.
 */
public interface FirebaseAuthService {

    /**
     * Verifies the given ID token and returns the resulting verified user.
     * Throws {@link br.com.bnuuy.jwar.server.exception.UnauthorizedException} on failure.
     */
    VerifiedToken verifyIdToken(String idToken);

    /**
     * Creates a new email/password Firebase user. Returns the created uid.
     */
    CreatedUser createUser(String email, String password, String displayName);

    /**
     * Mints a custom Firebase token for the given uid that the client can exchange for an ID token.
     */
    String createCustomToken(String uid);

    /**
     * Updates the Firebase user profile.
     */
    void updateUser(String uid, String displayName, String photoUrl);

    /**
     * Deletes the Firebase user.
     */
    void deleteUser(String uid);

    record VerifiedToken(String uid, String email, boolean emailVerified, String name, String provider) {}

    record CreatedUser(String uid, String email, String displayName) {}
}
