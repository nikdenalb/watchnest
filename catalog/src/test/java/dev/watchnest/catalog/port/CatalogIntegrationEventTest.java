package dev.watchnest.catalog.port;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.TitleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogIntegrationEventTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TITLE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Instant AT = Instant.parse("2026-08-25T18:00:00Z");
    private static final CatalogTitle TITLE = new CatalogTitle(
            TITLE_ID,
            TitleType.FILM,
            "Dune",
            "Dune",
            2021,
            null,
            null,
            null
    );

    @Test
    void createdRejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new CatalogTitleCreatedV1(null, AT, TITLE));
        assertThrows(NullPointerException.class, () -> new CatalogTitleCreatedV1(EVENT_ID, null, TITLE));
        assertThrows(NullPointerException.class, () -> new CatalogTitleCreatedV1(EVENT_ID, AT, null));
    }

    @Test
    void updatedRejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new CatalogTitleUpdatedV1(null, AT, TITLE));
        assertThrows(NullPointerException.class, () -> new CatalogTitleUpdatedV1(EVENT_ID, null, TITLE));
        assertThrows(NullPointerException.class, () -> new CatalogTitleUpdatedV1(EVENT_ID, AT, null));
    }

    @Test
    void deletedRejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new CatalogTitleDeletedV1(null, AT, TITLE));
        assertThrows(NullPointerException.class, () -> new CatalogTitleDeletedV1(EVENT_ID, null, TITLE));
        assertThrows(NullPointerException.class, () -> new CatalogTitleDeletedV1(EVENT_ID, AT, null));
    }
}
