package br.com.bnuuy.jwar.server.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Catch-all route that forwards non-API browser GETs to /index.html so React
 * Router can take over and render client-side routes (deep-link refresh of
 * /lobby, /rooms/:id, /matches/:id stays a 200 instead of a 404).
 *
 * Spec 007 FR-021. Excludes /api/**, /ws/**, /swagger-ui/**, /v3/api-docs/**,
 * /actuator/**, /h2-console/** which are real server endpoints.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/signup",
        "/lobby",
        "/lobby/**",
        "/rooms/**",
        "/matches/**",
        "/me",
        "/me/**",
        "/goodbye"
    })
    @GetMapping
    public String forward(HttpServletRequest request) {
        return "forward:/index.html";
    }
}
