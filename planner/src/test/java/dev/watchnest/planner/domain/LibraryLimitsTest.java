package dev.watchnest.planner.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryLimitsTest {

    @Test
    void maxTitlesPerDateIsFifty() {
        assertEquals(50, LibraryLimits.MAX_TITLES_PER_DATE);
    }
}
