package dev.watchnest.catalog.port;

import dev.watchnest.catalog.domain.CatalogTitle;

import java.time.Instant;
import java.util.UUID;

public sealed interface CatalogIntegrationEvent
        permits CatalogTitleCreatedV1, CatalogTitleUpdatedV1, CatalogTitleDeletedV1 {

    UUID eventId();

    Instant occurredAt();

    CatalogTitle title();
}
