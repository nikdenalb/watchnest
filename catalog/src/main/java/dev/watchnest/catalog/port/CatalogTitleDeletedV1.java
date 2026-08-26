package dev.watchnest.catalog.port;

import dev.watchnest.catalog.domain.CatalogTitle;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CatalogTitleDeletedV1(UUID eventId, Instant occurredAt, CatalogTitle title)
        implements CatalogIntegrationEvent {

    public CatalogTitleDeletedV1 {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(title, "title");
    }
}
