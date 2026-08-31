package dev.watchnest.plannerapp.catalog.config;

import dev.watchnest.catalog.port.CatalogIntegrationEventPublisher;
import dev.watchnest.catalog.port.CatalogTitleRepository;
import dev.watchnest.catalog.service.CatalogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class CatalogConfig {

    @Bean
    CatalogService catalogService(
            CatalogTitleRepository catalogTitleRepository,
            CatalogIntegrationEventPublisher catalogIntegrationEventPublisher,
            Clock clock
    ) {
        return new CatalogService(
                catalogTitleRepository,
                catalogIntegrationEventPublisher,
                clock,
                UUID::randomUUID,
                UUID::randomUUID
        );
    }
}
