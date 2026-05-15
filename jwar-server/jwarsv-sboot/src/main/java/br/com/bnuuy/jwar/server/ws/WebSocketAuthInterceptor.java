package br.com.bnuuy.jwar.server.ws;

import br.com.bnuuy.jwar.server.auth.FirebaseAuthService;
import br.com.bnuuy.jwar.server.auth.FirebasePrincipal;
import br.com.bnuuy.jwar.server.domain.User;
import br.com.bnuuy.jwar.server.service.UserService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP CONNECT frames using the Firebase ID token passed via the
 * native {@code Authorization: Bearer <token>} header (or, for browser clients,
 * the {@code token} query parameter on the handshake).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token == null) {
                throw new IllegalArgumentException("Token de autenticação ausente");
            }
            try {
                FirebaseAuthService.VerifiedToken verified = firebaseAuthService.verifyIdToken(token);
                User user = userService.bootstrap(verified);
                FirebasePrincipal fp = new FirebasePrincipal(
                    user.getId(),
                    verified.uid(),
                    verified.email(),
                    user.getDisplayName(),
                    verified.provider(),
                    token
                );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    fp, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                accessor.setUser(auth);
                log.debug("WebSocket CONNECT authenticated: userId={}", user.getId());
            } catch (Exception ex) {
                log.warn("WebSocket CONNECT auth failed: {}", ex.getMessage());
                throw new IllegalArgumentException("Falha na autenticação WebSocket");
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length()).trim();
        }
        String token = accessor.getFirstNativeHeader("token");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        Principal user = accessor.getUser();
        return user != null ? user.getName() : null;
    }
}
