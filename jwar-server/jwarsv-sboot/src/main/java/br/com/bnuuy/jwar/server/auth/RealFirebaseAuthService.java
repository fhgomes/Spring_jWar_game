package br.com.bnuuy.jwar.server.auth;

import br.com.bnuuy.jwar.server.exception.ConflictException;
import br.com.bnuuy.jwar.server.exception.UnauthorizedException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RealFirebaseAuthService implements FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;

    @Override
    public VerifiedToken verifyIdToken(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
            String provider = Optional.ofNullable(token.getClaims().get("firebase"))
                .filter(java.util.Map.class::isInstance)
                .map(m -> (String) ((java.util.Map<?, ?>) m).get("sign_in_provider"))
                .orElse("password");
            return new VerifiedToken(
                token.getUid(),
                token.getEmail(),
                token.isEmailVerified(),
                token.getName(),
                provider
            );
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase token verification failed: {}", ex.getMessage());
            String code = mapErrorCode(ex);
            throw new UnauthorizedException(toPtBr(code), code);
        }
    }

    @Override
    public CreatedUser createUser(String email, String password, String displayName) {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName)
                .setEmailVerified(false);
            UserRecord record = firebaseAuth.createUser(request);
            return new CreatedUser(record.getUid(), record.getEmail(), record.getDisplayName());
        } catch (FirebaseAuthException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("already")) {
                throw new ConflictException("E-mail já cadastrado", "EMAIL_ALREADY_IN_USE");
            }
            throw new ConflictException("Falha ao criar usuário no Firebase: " + ex.getMessage(), "FIREBASE_ERROR");
        }
    }

    @Override
    public String createCustomToken(String uid) {
        try {
            return firebaseAuth.createCustomToken(uid);
        } catch (FirebaseAuthException ex) {
            throw new ConflictException("Falha ao gerar token customizado", "FIREBASE_ERROR");
        }
    }

    @Override
    public void updateUser(String uid, String displayName, String photoUrl) {
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid);
            if (displayName != null) {
                request.setDisplayName(displayName);
            }
            if (photoUrl != null && !photoUrl.isBlank()) {
                request.setPhotoUrl(photoUrl);
            }
            firebaseAuth.updateUser(request);
        } catch (FirebaseAuthException ex) {
            log.warn("Failed to update Firebase user {}: {}", uid, ex.getMessage());
        }
    }

    @Override
    public void deleteUser(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException ex) {
            log.warn("Failed to delete Firebase user {}: {}", uid, ex.getMessage());
        }
    }

    private String mapErrorCode(FirebaseAuthException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("expired")) {
            return "TOKEN_EXPIRED";
        }
        if (message.contains("revoked")) {
            return "TOKEN_REVOKED";
        }
        return "INVALID_TOKEN";
    }

    private String toPtBr(String code) {
        return switch (code) {
            case "TOKEN_EXPIRED" -> "Token expirado";
            case "TOKEN_REVOKED" -> "Token revogado";
            default -> "Token inválido";
        };
    }
}
