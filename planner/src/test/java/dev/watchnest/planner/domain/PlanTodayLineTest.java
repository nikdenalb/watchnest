package dev.watchnest.planner.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanTodayLineTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    void trimsContentTitle() {
        PlanTodayLine line = new PlanTodayLine(ID, "  Episode  ", false, PlanLineSource.FORWARD);

        assertEquals("Episode", line.contentTitle());
    }

    @Test
    void acceptsTitleAtMaxLength() {
        String title = "b".repeat(120);

        PlanTodayLine line = new PlanTodayLine(ID, title, true, PlanLineSource.MANUAL);

        assertEquals(title, line.contentTitle());
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlanTodayLine(ID, "  ", false, PlanLineSource.MANUAL));
        assertThrows(IllegalArgumentException.class,
                () -> new PlanTodayLine(ID, "", false, PlanLineSource.MANUAL));
    }

    @Test
    void rejectsTitleOverMaxLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlanTodayLine(ID, "b".repeat(121), false, PlanLineSource.MANUAL));
    }

    @Test
    void requiresIdAndSource() {
        assertThrows(NullPointerException.class,
                () -> new PlanTodayLine(null, "Show", false, PlanLineSource.MANUAL));
        assertThrows(NullPointerException.class,
                () -> new PlanTodayLine(ID, "Show", false, null));
        assertThrows(NullPointerException.class,
                () -> new PlanTodayLine(ID, null, false, PlanLineSource.MANUAL));
    }

    @Test
    void withCheckedKeepsIdTitleAndSource() {
        PlanTodayLine line = new PlanTodayLine(ID, "Show", false, PlanLineSource.FORWARD);

        PlanTodayLine checked = line.withChecked(true);

        assertEquals(ID, checked.id());
        assertEquals("Show", checked.contentTitle());
        assertEquals(PlanLineSource.FORWARD, checked.source());
        assertEquals(true, checked.checked());
    }
}
