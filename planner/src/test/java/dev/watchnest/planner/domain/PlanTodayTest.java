package dev.watchnest.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanTodayTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
    private static final UUID FIRST_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID THIRD_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

    @Test
    void requiresOwnerAndDate() {
        assertThrows(NullPointerException.class, () -> new PlanToday(null, TODAY, List.of()));
        assertThrows(NullPointerException.class, () -> new PlanToday(OWNER_ID, null, List.of()));
        assertThrows(NullPointerException.class, () -> new PlanToday(OWNER_ID, TODAY, null));
    }

    @Test
    void preservesConstructorLineOrder() {
        PlanTodayLine first = line(FIRST_ID, "Moved A", PlanLineSource.FORWARD);
        PlanTodayLine second = line(SECOND_ID, "Moved B", PlanLineSource.FORWARD);

        PlanToday plan = new PlanToday(OWNER_ID, TODAY, List.of(first, second));

        assertEquals(List.of(FIRST_ID, SECOND_ID), plan.lines().stream().map(PlanTodayLine::id).toList());
    }

    @Test
    void appendPutsManualLineAfterMovedLines() {
        PlanTodayLine moved = line(FIRST_ID, "From forward", PlanLineSource.FORWARD);
        PlanTodayLine manual = line(SECOND_ID, "Added today", PlanLineSource.MANUAL);

        PlanToday plan = PlanToday.empty(OWNER_ID, TODAY).append(moved).append(manual);

        assertEquals(List.of(FIRST_ID, SECOND_ID), plan.lines().stream().map(PlanTodayLine::id).toList());
        assertEquals(PlanLineSource.FORWARD, plan.lines().get(0).source());
        assertEquals(PlanLineSource.MANUAL, plan.lines().get(1).source());
    }

    @Test
    void removePreservesRelativeOrderOfRemainingLines() {
        PlanToday plan = new PlanToday(OWNER_ID, TODAY, List.of(
                line(FIRST_ID, "A", PlanLineSource.FORWARD),
                line(SECOND_ID, "B", PlanLineSource.MANUAL),
                line(THIRD_ID, "C", PlanLineSource.MANUAL)
        ));

        PlanToday afterRemove = plan.removeLine(SECOND_ID);

        assertEquals(List.of(FIRST_ID, THIRD_ID), afterRemove.lines().stream().map(PlanTodayLine::id).toList());
    }

    @Test
    void withLineCheckedKeepsOrder() {
        PlanToday plan = new PlanToday(OWNER_ID, TODAY, List.of(
                line(FIRST_ID, "A", PlanLineSource.FORWARD),
                line(SECOND_ID, "B", PlanLineSource.MANUAL)
        ));

        PlanToday checked = plan.withLineChecked(FIRST_ID, true);

        assertEquals(List.of(FIRST_ID, SECOND_ID), checked.lines().stream().map(PlanTodayLine::id).toList());
        assertEquals(true, checked.lines().get(0).checked());
        assertEquals(false, checked.lines().get(1).checked());
    }

    @Test
    void withLineCheckedRejectsUnknownId() {
        PlanToday plan = PlanToday.empty(OWNER_ID, TODAY)
                .append(line(FIRST_ID, "A", PlanLineSource.MANUAL));

        assertThrows(IllegalArgumentException.class, () -> plan.withLineChecked(SECOND_ID, true));
    }

    private static PlanTodayLine line(UUID id, String title, PlanLineSource source) {
        return new PlanTodayLine(id, title, false, source);
    }
}
