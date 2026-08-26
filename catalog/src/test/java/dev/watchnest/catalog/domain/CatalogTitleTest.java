package dev.watchnest.catalog.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogTitleTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void definesExactlyFourTypes() {
        assertArrayEquals(
                new TitleType[] {TitleType.FILM, TitleType.TV_SERIES, TitleType.MINI_SERIES, TitleType.TV_SHOW},
                TitleType.values()
        );
    }

    @ParameterizedTest
    @EnumSource(TitleType.class)
    void acceptsEveryTitleType(TitleType type) {
        CatalogTitle title = title(type, "Dune", "Dune", 2021, null, null, null);

        assertEquals(type, title.type());
        assertEquals(ID, title.id());
    }

    @Test
    void trimsNamesAndPreservesCaseAndInternalWhitespace() {
        CatalogTitle title = title(
                TitleType.FILM,
                "  The  Dune  ",
                "  Дюна  ",
                2021,
                null,
                null,
                null
        );

        assertEquals("The  Dune", title.nameEn());
        assertEquals("Дюна", title.nameOriginal());
        assertEquals("the  dune", title.nameEnKey());
    }

    @Test
    void nameEnKeyUsesLocaleRoot() {
        CatalogTitle title = title(TitleType.FILM, "TITLE", "TITLE", 2021, null, null, null);

        assertEquals("title", title.nameEnKey());
        assertEquals("TITLE".toLowerCase(Locale.ROOT), title.nameEnKey());
    }

    @Test
    void acceptsNameLengthBoundaries() {
        String max = "x".repeat(255);
        CatalogTitle title = title(TitleType.TV_SHOW, max, max, 1000, null, null, null);

        assertEquals(max, title.nameEn());
        assertEquals(max, title.nameOriginal());
        assertEquals(1000, title.year());
    }

    @Test
    void acceptsYearBoundaries() {
        assertEquals(1000, title(TitleType.FILM, "A", "A", 1000, null, null, null).year());
        assertEquals(9999, title(TitleType.FILM, "A", "A", 9999, null, null, null).year());
    }

    @Test
    void rejectsMissingBlankAndOversizedNames() {
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, null, "A", 2021, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", null, 2021, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "  ", "A", 2021, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "  ", 2021, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "x".repeat(256), "A", 2021, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "x".repeat(256), 2021, null, null, null));
    }

    @Test
    void rejectsYearOutsideFourDigitRange() {
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 999, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 10000, null, null, null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 0, null, null, null));
    }

    @Test
    void requiresIdAndType() {
        assertThrows(NullPointerException.class,
                () -> new CatalogTitle(null, TitleType.FILM, "A", "A", 2021, null, null, null));
        assertThrows(NullPointerException.class,
                () -> title(null, "A", "A", 2021, null, null, null));
    }

    @Test
    void blankOptionalFieldsBecomeNull() {
        CatalogTitle title = title(
                TitleType.MINI_SERIES,
                "Chernobyl",
                "Chernobyl",
                2019,
                "   ",
                "  ,  , ",
                ""
        );

        assertNull(title.description());
        assertNull(title.genres());
        assertNull(title.countries());
    }

    @Test
    void nullOptionalFieldsStayNull() {
        CatalogTitle title = title(TitleType.TV_SERIES, "Lost", "Lost", 2004, null, null, null);

        assertNull(title.description());
        assertNull(title.genres());
        assertNull(title.countries());
    }

    @Test
    void trimsDescriptionAndPreservesCase() {
        CatalogTitle title = title(
                TitleType.FILM,
                "Dune",
                "Dune",
                2021,
                "  An Epic  ",
                null,
                null
        );

        assertEquals("An Epic", title.description());
    }

    @Test
    void acceptsDescriptionAtMaxLengthAndRejectsOver() {
        String max = "d".repeat(10_000);
        assertEquals(max, title(TitleType.FILM, "A", "A", 2021, max, null, null).description());
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 2021, "d".repeat(10_001), null, null));
    }

    @Test
    void canonicalizesGenresAndCountries() {
        CatalogTitle title = title(
                TitleType.FILM,
                "Dune",
                "Dune",
                2021,
                null,
                " Drama,  Sci-Fi,,Drama, ",
                " United States, Canada,United States "
        );

        assertEquals("drama, sci-fi, drama", title.genres());
        assertEquals("united states, canada, united states", title.countries());
    }

    @Test
    void tagCanonicalizationUsesLocaleRootAndPreservesTokenOrder() {
        CatalogTitle title = title(
                TitleType.FILM,
                "A",
                "A",
                2021,
                null,
                "I, SCIENCE Fiction",
                "TURKEY, United Kingdom"
        );

        assertEquals("i, science fiction", title.genres());
        assertEquals("turkey, united kingdom", title.countries());
        assertEquals("I".toLowerCase(Locale.ROOT) + ", science fiction", title.genres());
    }

    @Test
    void acceptsTagListAtMaxCanonicalLengthAndRejectsOver() {
        String max = "g".repeat(1000);
        assertEquals(max, title(TitleType.FILM, "A", "A", 2021, null, max, null).genres());
        assertEquals(max, title(TitleType.FILM, "A", "A", 2021, null, null, max).countries());
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 2021, null, "g".repeat(1001), null));
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 2021, null, null, "c".repeat(1001)));
    }

    @Test
    void rejectsTagListWhoseJoinedCanonicalFormExceedsMax() {
        String tooLong = "ab, " + "c".repeat(997);
        assertEquals(1001, tooLong.length());
        assertThrows(InvalidCatalogTitleException.class,
                () -> title(TitleType.FILM, "A", "A", 2021, null, tooLong, null));
    }

    private static CatalogTitle title(
            TitleType type,
            String nameEn,
            String nameOriginal,
            int year,
            String description,
            String genres,
            String countries
    ) {
        return new CatalogTitle(ID, type, nameEn, nameOriginal, year, description, genres, countries);
    }
}
