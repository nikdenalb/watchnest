package dev.watchnest.catalog.port;

public interface CatalogIntegrationEventPublisher {

    void publish(CatalogIntegrationEvent event);
}
