package dev.watchnest.catalog.service;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.CatalogTitleNotFoundException;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.port.CatalogIntegrationEventPublisher;
import dev.watchnest.catalog.port.CatalogTitleCreatedV1;
import dev.watchnest.catalog.port.CatalogTitleDeletedV1;
import dev.watchnest.catalog.port.CatalogTitleRepository;
import dev.watchnest.catalog.port.CatalogTitleUpdatedV1;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class CatalogService {

    private final CatalogTitleRepository titles;
    private final CatalogIntegrationEventPublisher events;
    private final Clock clock;
    private final Supplier<UUID> titleIdGenerator;
    private final Supplier<UUID> eventIdGenerator;

    public CatalogService(
            CatalogTitleRepository titles,
            CatalogIntegrationEventPublisher events,
            Clock clock,
            Supplier<UUID> titleIdGenerator,
            Supplier<UUID> eventIdGenerator
    ) {
        this.titles = Objects.requireNonNull(titles, "titles");
        this.events = Objects.requireNonNull(events, "events");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.titleIdGenerator = Objects.requireNonNull(titleIdGenerator, "titleIdGenerator");
        this.eventIdGenerator = Objects.requireNonNull(eventIdGenerator, "eventIdGenerator");
    }

    public CatalogTitle create(
            TitleType type,
            String nameEn,
            String nameOriginal,
            int year,
            String description,
            String genres,
            String countries
    ) {
        CatalogTitle title = new CatalogTitle(
                titleIdGenerator.get(),
                type,
                nameEn,
                nameOriginal,
                year,
                description,
                genres,
                countries
        );
        titles.insert(title);
        events.publish(new CatalogTitleCreatedV1(eventIdGenerator.get(), Instant.now(clock), title));
        return title;
    }

    public CatalogTitle get(UUID id) {
        Objects.requireNonNull(id, "id");
        return titles.findById(id).orElseThrow(() -> new CatalogTitleNotFoundException(id));
    }

    public List<CatalogTitle> list() {
        return titles.findAllSorted();
    }

    public List<CatalogTitle> search(String query) {
        return titles.findByNameEnContainingLiteral(query);
    }

    public CatalogTitle update(
            UUID id,
            TitleType type,
            String nameEn,
            String nameOriginal,
            int year,
            String description,
            String genres,
            String countries
    ) {
        Objects.requireNonNull(id, "id");
        if (titles.findById(id).isEmpty()) {
            throw new CatalogTitleNotFoundException(id);
        }
        CatalogTitle replacement = new CatalogTitle(
                id,
                type,
                nameEn,
                nameOriginal,
                year,
                description,
                genres,
                countries
        );
        titles.update(replacement);
        events.publish(new CatalogTitleUpdatedV1(eventIdGenerator.get(), Instant.now(clock), replacement));
        return replacement;
    }

    public void delete(UUID id) {
        Objects.requireNonNull(id, "id");
        CatalogTitle existing = titles.findById(id)
                .orElseThrow(() -> new CatalogTitleNotFoundException(id));
        titles.delete(id);
        events.publish(new CatalogTitleDeletedV1(eventIdGenerator.get(), Instant.now(clock), existing));
    }
}
