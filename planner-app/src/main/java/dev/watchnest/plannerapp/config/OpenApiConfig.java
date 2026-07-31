package dev.watchnest.plannerapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI watchNestOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("WatchNest Planner API")
                        .description("REST API for the personal watch library")
                        .version("v1"));
    }
}
