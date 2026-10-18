package br.com.bnuuy.jwar.server.auth;

import br.com.bnuuy.jwar.server.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to retrieve the authenticated FirebasePrincipal from the SecurityContext.
 */
@Component
public class CurrentUser {

    public FirebasePrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof FirebasePrincipal principal)) {
            throw new UnauthorizedException("Não autenticado", "UNAUTHENTICATED");
        }
        return principal;
    }

    public UUID userId() {
        return get().getUserId();
    }

    public String firebaseUid() {
        return get().getFirebaseUid();
    }
}
