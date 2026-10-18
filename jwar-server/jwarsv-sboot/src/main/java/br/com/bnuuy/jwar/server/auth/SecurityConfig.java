package br.com.bnuuy.jwar.server.auth;

import br.com.bnuuy.jwar.server.api.advice.CorrelationIdFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Inject as Filter interface (not concrete FirebaseAuthFilter) because Spring
    // Modulith / async support may wrap the bean in a JDK dynamic proxy that loses
    // the concrete type. The proxy still implements jakarta.servlet.Filter.
    @Qualifier("firebaseAuthFilter")
    private final Filter firebaseAuthFilter;

    @Qualifier("correlationIdFilter")
    private final Filter correlationIdFilter;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public FilterRegistrationBean<Filter> correlationIdFilterRegistration() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(correlationIdFilter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        // OncePerRequestFilter guards against double-invocation, but keep auto-registration off
        // to avoid the auto-registration bean and this one fighting for ordering.
        return reg;
    }

    @Bean
    public FilterRegistrationBean<Filter> firebaseAuthFilterRegistration() {
        // Disable Spring Boot's auto-registration so this filter only runs via the Spring Security chain.
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>(firebaseAuthFilter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            cors.setAllowedOrigins(List.of("http://localhost:5173"));
        } else {
            cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        }
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        cors.setExposedHeaders(List.of("X-Correlation-Id"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    // Static SPA assets and React Router paths — index.html
                    // is served either directly or via SpaController forward.
                    "/",
                    "/index.html",
                    "/favicon.ico",
                    "/favicon.svg",
                    "/assets/**",
                    "/static/**",
                    "/login",
                    "/signup",
                    "/lobby",
                    "/lobby/**",
                    "/rooms/**",
                    "/matches/**",
                    "/me",
                    "/me/**",
                    "/goodbye",
                    // Public API + tooling
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
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
