package dev.watchnest.plannerapp.catalog.event;

import dev.watchnest.catalog.port.CatalogIntegrationEvent;
import dev.watchnest.catalog.port.CatalogIntegrationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("memory")
public class LoggingCatalogIntegrationEventPublisher implements CatalogIntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingCatalogIntegrationEventPublisher.class);

    @Override
    public void publish(CatalogIntegrationEvent event) {
        log.info("catalog-integration-event {}", event);
    }
}
