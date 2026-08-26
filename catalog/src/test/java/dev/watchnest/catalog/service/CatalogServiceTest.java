package dev.watchnest.catalog.service;

import dev.watchnest.catalog.adapter.memory.InMemoryCatalogTitleRepository;
import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.CatalogTitleNotFoundException;
import dev.watchnest.catalog.domain.DuplicateCatalogTitleException;
import dev.watchnest.catalog.domain.InvalidCatalogTitleException;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.port.CatalogTitleCreatedV1;
import dev.watchnest.catalog.port.CatalogTitleDeletedV1;
import dev.watchnest.catalog.port.CatalogTitleUpdatedV1;
import dev.watchnest.catalog.support.RecordingCatalogIntegrationEventPublisher;
import dev.watchnest.catalog.support.SequentialUuidSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-25T18:00:00Z");
    private static final UUID TITLE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MISSING_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private InMemoryCatalogTitleRepository titles;
    private RecordingCatalogIntegrationEventPublisher events;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        titles = new InMemoryCatalogTitleRepository();
        events = new RecordingCatalogIntegrationEventPublisher();
        service = newService(() -> TITLE_ID, () -> EVENT_ID);
    }

    @Test
    void createNormalizesSnapshotAndPublishesCreatedEvent() {
        CatalogTitle created = service.create(
                TitleType.FILM,
                "  Dune  ",
                "  Dune  ",
                2021,
                "  Epic  ",
                " Drama, Sci-Fi ",
                " United States "
        );

        assertEquals(TITLE_ID, created.id());
        assertEquals(TitleType.FILM, created.type());
        assertEquals("Dune", created.nameEn());
        assertEquals("dune", created.nameEnKey());
        assertEquals("Dune", created.nameOriginal());
        assertEquals(2021, created.year());
        assertEquals("Epic", created.description());
        assertEquals("drama, sci-fi", created.genres());
        assertEquals("united states", created.countries());
        assertEquals(created, service.get(TITLE_ID));

        assertEquals(1, events.events().size());
        CatalogTitleCreatedV1 event = assertInstanceOf(CatalogTitleCreatedV1.class, events.events().getFirst());
        assertEquals(EVENT_ID, event.eventId());
        assertEquals(FIXED_INSTANT, event.occurredAt());
        assertEquals(created, event.title());
    }

    @Test
    void createRejectsCaseInsensitiveDuplicateAndReturnsExistingTitle() {
        CatalogTitle existing = service.create(TitleType.FILM, "Dune", "Dune", 2021, null, null, null);
        CatalogService second = newService(
                () -> UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                () -> UUID.fromString("22222222-2222-2222-2222-222222222222")
        );

        DuplicateCatalogTitleException duplicate = assertThrows(
                DuplicateCatalogTitleException.class,
                () -> second.create(TitleType.FILM, "  DUNE  ", "Other", 2021, "desc", "drama", "usa")
        );

        assertEquals(existing, duplicate.existingTitle());
        assertEquals(existing, service.get(TITLE_ID));
        assertTrue(titles.findById(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")).isEmpty());
        assertEquals(1, events.events().size());
        assertInstanceOf(CatalogTitleCreatedV1.class, events.events().getFirst());
    }

    @Test
    void sameEnglishKeyDifferentYearOrTypeAreDistinctWorks() {
        CatalogService sequential = newService(new SequentialUuidSupplier(), new SequentialUuidSupplier());

        CatalogTitle film2021 = sequential.create(TitleType.FILM, "Dune", "Dune", 2021, null, null, null);
        CatalogTitle film2024 = sequential.create(TitleType.FILM, "dune", "Dune", 2024, null, null, null);
        CatalogTitle series = sequential.create(TitleType.TV_SERIES, "DUNE", "Dune", 2021, null, null, null);

        assertNotEquals(film2021.id(), film2024.id());
        assertNotEquals(film2021.id(), series.id());
        assertEquals(3, sequential.list().size());
        assertEquals(3, events.events().size());
    }

    @Test
    void createAcceptsAllFourTypesAsDistinctWorks() {
        CatalogService sequential = newService(new SequentialUuidSupplier(), new SequentialUuidSupplier());
        for (TitleType type : TitleType.values()) {
            sequential.create(type, "Shared", "Shared", 2021, null, null, null);
        }

        List<CatalogTitle> listed = sequential.list();
        assertEquals(4, listed.size());
        assertEquals(
                List.of(TitleType.FILM, TitleType.TV_SERIES, TitleType.MINI_SERIES, TitleType.TV_SHOW),
                listed.stream().map(CatalogTitle::type).toList()
        );
        assertEquals(4, events.events().size());
        assertTrue(events.events().stream().allMatch(CatalogTitleCreatedV1.class::isInstance));
    }

    @Test
    void updateReplacesFieldsPreservesIdAndPublishesUpdatedEvent() {
        service.create(TitleType.FILM, "Dune", "Dune", 2021, null, null, null);
        CatalogService updating = newService(() -> TITLE_ID, () -> UUID.fromString("22222222-2222-2222-2222-222222222222"));

        CatalogTitle updated = updating.update(
                TITLE_ID,
                TitleType.MINI_SERIES,
                "  Chernobyl  ",
                "  Чернобыль  ",
                2019,
                "  Mini  ",
                " Drama ",
                " United Kingdom "
        );

        assertEquals(TITLE_ID, updated.id());
        assertEquals(TitleType.MINI_SERIES, updated.type());
        assertEquals("Chernobyl", updated.nameEn());
        assertEquals("chernobyl", updated.nameEnKey());
        assertEquals("Чернобыль", updated.nameOriginal());
        assertEquals(2019, updated.year());
        assertEquals("Mini", updated.description());
        assertEquals("drama", updated.genres());
        assertEquals("united kingdom", updated.countries());
        assertEquals(updated, service.get(TITLE_ID));

        assertEquals(2, events.events().size());
        CatalogTitleUpdatedV1 event = assertInstanceOf(CatalogTitleUpdatedV1.class, events.events().get(1));
        assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), event.eventId());
        assertEquals(FIXED_INSTANT, event.occurredAt());
        assertEquals(updated, event.title());
    }

    @Test
    void updateCaseOnlyEnglishNameDoesNotCreateASecondWork() {
        service.create(TitleType.FILM, "Dune", "Dune", 2021, "old", null, null);

        CatalogTitle updated = service.update(
                TITLE_ID,
                TitleType.FILM,
                "DUNE",
                "Dune",
                2021,
                "new",
                null,
                null
        );

        assertEquals(TITLE_ID, updated.id());
        assertEquals("DUNE", updated.nameEn());
        assertEquals("dune", updated.nameEnKey());
        assertEquals("new", updated.description());
        assertEquals(1, service.list().size());
        assertInstanceOf(CatalogTitleUpdatedV1.class, events.events().get(1));
    }

    @Test
    void updateCollisionReturnsExistingTitleAndPublishesNoUpdateEvent() {
        CatalogService sequential = newService(new SequentialUuidSupplier(), new SequentialUuidSupplier());
        CatalogTitle dune = sequential.create(TitleType.FILM, "Dune", "Dune", 2021, null, null, null);
        CatalogTitle other = sequential.create(TitleType.FILM, "Other", "Other", 2020, null, null, null);
        int published = events.events().size();

        DuplicateCatalogTitleException duplicate = assertThrows(
                DuplicateCatalogTitleException.class,
                () -> sequential.update(
                        other.id(),
                        TitleType.FILM,
                        "dune",
                        "Changed",
                        2021,
                        null,
                        null,
                        null
                )
        );

        assertEquals(dune, duplicate.existingTitle());
        assertEquals("Other", sequential.get(other.id()).nameEn());
        assertEquals(published, events.events().size());
    }

    @Test
    void listAndSearchAreSortedAndTreatPercentAndUnderscoreLiterally() {
        CatalogService sequential = newService(new SequentialUuidSupplier(), new SequentialUuidSupplier());
        CatalogTitle alphaFilm = sequential.create(TitleType.FILM, "Alpha", "Alpha", 2020, null, null, null);
        CatalogTitle alphaSeries = sequential.create(TitleType.TV_SERIES, "ALPHA", "Alpha", 2020, null, null, null);
        CatalogTitle alphaLater = sequential.create(TitleType.FILM, "Alpha", "Alpha", 2021, null, null, null);
        CatalogTitle percent = sequential.create(TitleType.FILM, "100% Wolf", "100% Wolf", 2020, null, null, null);
        CatalogTitle underscore = sequential.create(TitleType.FILM, "Under_score", "Under_score", 2020, null, null, null);
        CatalogTitle beta = sequential.create(TitleType.FILM, "beta", "beta", 2020, null, null, null);

        List<CatalogTitle> listed = sequential.list();
        assertEquals(
                List.of(percent, alphaFilm, alphaSeries, alphaLater, beta, underscore),
                listed
        );
        assertEquals(listed, sequential.search("  "));
        assertEquals(listed, sequential.search(null));

        assertEquals(List.of(alphaFilm, alphaSeries, alphaLater), sequential.search("  ALP  "));
        assertEquals(List.of(percent), sequential.search("100%"));
        assertEquals(List.of(percent), sequential.search("%"));
        assertEquals(List.of(underscore), sequential.search("_"));
        assertTrue(sequential.search("100_").isEmpty());
        assertTrue(sequential.search("under%").isEmpty());
        assertTrue(sequential.search("dune").isEmpty());
    }

    @Test
    void missingGetUpdateAndDeleteThrowNotFoundAndPublishNothing() {
        assertThrows(CatalogTitleNotFoundException.class, () -> service.get(MISSING_ID));
        assertThrows(
                CatalogTitleNotFoundException.class,
                () -> service.update(MISSING_ID, TitleType.FILM, "A", "A", 2021, null, null, null)
        );
        assertThrows(CatalogTitleNotFoundException.class, () -> service.delete(MISSING_ID));
        assertTrue(events.events().isEmpty());
        assertTrue(service.list().isEmpty());
    }

    @Test
    void deleteIsHardDeletePublishesDeletedSnapshotAndFreesTheNaturalKey() {
        CatalogTitle created = service.create(
                TitleType.FILM,
                "  Dune  ",
                "Dune",
                2021,
                "Epic",
                "drama",
                "usa"
        );
        CatalogService deleting = newService(
                () -> UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                () -> UUID.fromString("22222222-2222-2222-2222-222222222222")
        );

        deleting.delete(TITLE_ID);

        assertThrows(CatalogTitleNotFoundException.class, () -> service.get(TITLE_ID));
        assertTrue(service.list().isEmpty());
        CatalogTitleDeletedV1 deleted = assertInstanceOf(CatalogTitleDeletedV1.class, events.events().get(1));
        assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), deleted.eventId());
        assertEquals(FIXED_INSTANT, deleted.occurredAt());
        assertEquals(created, deleted.title());

        CatalogTitle recreated = deleting.create(TitleType.FILM, "Dune", "Dune", 2021, null, null, null);
        assertEquals(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), recreated.id());
        assertEquals(3, events.events().size());
        assertInstanceOf(CatalogTitleCreatedV1.class, events.events().get(2));
    }

    @Test
    void rejectedWritesPublishNoEvent() {
        assertThrows(
                InvalidCatalogTitleException.class,
                () -> service.create(TitleType.FILM, "  ", "A", 2021, null, null, null)
        );
        assertThrows(
                InvalidCatalogTitleException.class,
                () -> service.create(TitleType.FILM, "A", "A", 999, null, null, null)
        );
        service.create(TitleType.FILM, "Dune", "Dune", 2021, null, null, null);
        int published = events.events().size();
        assertThrows(
                InvalidCatalogTitleException.class,
                () -> service.update(TITLE_ID, TitleType.FILM, "A", "A", 10_000, null, null, null)
        );
        assertEquals(published, events.events().size());
        assertEquals("Dune", service.get(TITLE_ID).nameEn());
    }

    private CatalogService newService(Supplier<UUID> titleIds, Supplier<UUID> eventIds) {
        return new CatalogService(
                titles,
                events,
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
                titleIds,
                eventIds
        );
    }
}
