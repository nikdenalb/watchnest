package dev.watchnest.catalog.support;

import dev.watchnest.catalog.port.CatalogIntegrationEvent;
import dev.watchnest.catalog.port.CatalogIntegrationEventPublisher;

import java.util.ArrayList;
import java.util.List;

public final class RecordingCatalogIntegrationEventPublisher implements CatalogIntegrationEventPublisher {

    private final List<CatalogIntegrationEvent> events = new ArrayList<>();

    @Override
    public void publish(CatalogIntegrationEvent event) {
        events.add(event);
    }

    public List<CatalogIntegrationEvent> events() {
        return List.copyOf(events);
    }
}
