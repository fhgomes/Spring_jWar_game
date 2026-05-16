package br.com.bnuuy.jwar.server.auth;

import br.com.bnuuy.jwar.server.dto.ErrorResponse;
import br.com.bnuuy.jwar.server.exception.UnauthorizedException;
import br.com.bnuuy.jwar.server.repository.UserRepository;
import br.com.bnuuy.jwar.server.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATTERNS = List.of(
        "/api/health",
        "/api/auth/register",
        "/api/auth/login",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/v3/api-docs",
        "/actuator/health",
        "/actuator/info",
        "/h2-console/**",
        "/ws/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final FirebaseAuthService firebaseAuthService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only enforce Firebase auth on API endpoints. Static SPA assets and
        // client-side router paths (/, /login, /signup, /lobby, /rooms/*,
        // /matches/*, /me/*, etc.) must pass through so the React bundle and
        // SpaController forwards can serve index.html.
        boolean isApiPath = path.startsWith("/api/");
        if (!isApiPath) {
            return true;
        }
        return PUBLIC_PATTERNS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "UNAUTHENTICATED", "Token de autenticação ausente");
            return;
        }
        String token = header.substring("Bearer ".length()).trim();
        try {
            FirebaseAuthService.VerifiedToken verified = firebaseAuthService.verifyIdToken(token);
            br.com.bnuuy.jwar.server.domain.User user = userService.bootstrap(verified);
            FirebasePrincipal principal = new FirebasePrincipal(
                user.getId(),
                verified.uid(),
                verified.email(),
                user.getDisplayName(),
                verified.provider(),
                token
            );
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            MDC.put("userId", user.getId().toString());
            chain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            writeError(response, 401, ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Auth filter failure", ex);
            writeError(response, 401, "UNAUTHENTICATED", "Falha na autenticação");
        } finally {
            MDC.remove("userId");
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String correlationId = Optional.ofNullable(MDC.get("correlationId")).orElse("");
        ErrorResponse body = ErrorResponse.of(code, message, correlationId);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
