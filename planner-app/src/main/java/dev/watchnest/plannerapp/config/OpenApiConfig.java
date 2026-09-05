package dev.watchnest.plannerapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI watchNestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("WatchNest Planner API")
                        .description("Personal watch diary API. Session cookie `JSESSIONID`; CSRF via `GET /api/v1/auth/csrf`.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("sessionCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")));
    }
}
