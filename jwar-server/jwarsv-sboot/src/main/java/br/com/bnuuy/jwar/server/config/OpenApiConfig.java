package br.com.bnuuy.jwar.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jwarOpenAPI() {
        final String schemeName = "bearer-jwt";
        return new OpenAPI()
            .info(new Info()
                .title("jWar Server API")
                .description("Digital implementation of the classic Brazilian board game War (Risk-like). "
                    + "All authenticated endpoints expect a Firebase ID token in Authorization header.")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList(schemeName))
            .components(new Components()
                .addSecuritySchemes(schemeName, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("Firebase ID token")
                    .description("Firebase ID token obtained from the client SDK"))
            );
    }
}
