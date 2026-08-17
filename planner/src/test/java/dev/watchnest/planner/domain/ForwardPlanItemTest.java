package dev.watchnest.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForwardPlanItemTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDate PLANNED_FOR = LocalDate.of(2026, 8, 18);

    @Test
    void trimsContentTitle() {
        ForwardPlanItem item = new ForwardPlanItem(ID, OWNER_ID, PLANNED_FOR, "  Show  ");

        assertEquals("Show", item.contentTitle());
    }

    @Test
    void acceptsTitleAtMaxLength() {
        String title = "a".repeat(120);

        ForwardPlanItem item = new ForwardPlanItem(ID, OWNER_ID, PLANNED_FOR, title);

        assertEquals(title, item.contentTitle());
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new ForwardPlanItem(ID, OWNER_ID, PLANNED_FOR, "   "));
        assertThrows(IllegalArgumentException.class,
                () -> new ForwardPlanItem(ID, OWNER_ID, PLANNED_FOR, ""));
    }

    @Test
    void rejectsTitleOverMaxLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new ForwardPlanItem(ID, OWNER_ID, PLANNED_FOR, "a".repeat(121)));
    }

    @Test
    void requiresIdsAndDate() {
        assertThrows(NullPointerException.class,
                () -> new ForwardPlanItem(null, OWNER_ID, PLANNED_FOR, "Show"));
        assertThrows(NullPointerException.class,
                () -> new ForwardPlanItem(ID, null, PLANNED_FOR, "Show"));
        assertThrows(NullPointerException.class,
                () -> new ForwardPlanItem(ID, OWNER_ID, null, "Show"));
        assertThrows(NullPointerException.class,
                () -> new ForwardPlanItem(ID, OWNER_ID, PLANNED_FOR, null));
    }
}
