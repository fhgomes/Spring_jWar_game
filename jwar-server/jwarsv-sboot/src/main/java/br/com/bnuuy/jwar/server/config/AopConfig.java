package br.com.bnuuy.jwar.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Forces Spring to use CGLIB (subclass) proxies across the entire application context.
 * <p>
 * Without this, Spring Modulith's async event publication wraps several @Component
 * beans (FirebaseAuthFilter, WebSocketAuthInterceptor, ...) in JDK dynamic proxies
 * that only expose their declared interfaces — and the SecurityConfig /
 * WebSocketConfig classes inject them by concrete type. Result: startup fails
 * with "bean … could not be injected because it is a JDK dynamic proxy".
 * <p>
 * Setting proxyTargetClass=true at the three places that drive Spring AOP keeps
 * the subclass relationship intact so concrete-type injection continues to work.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAsync(proxyTargetClass = true)
@EnableTransactionManagement(proxyTargetClass = true)
public class AopConfig {
}
