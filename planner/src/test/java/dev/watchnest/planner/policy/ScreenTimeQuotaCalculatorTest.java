package dev.watchnest.planner.policy;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanLineSource;
import dev.watchnest.planner.domain.PlanTodayLine;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenTimeQuotaCalculatorTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 7, 6);
    private static final LocalDate WEEKEND = LocalDate.of(2026, 7, 4);

    private final ScreenTimeQuotaCalculator calculator = new ScreenTimeQuotaCalculator();

    @Test
    void emptyPlanLeavesFullRemainingQuota() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, List.of());

        assertEquals(WEEKDAY, status.date());
        assertEquals(2, status.episodeLimit());
        assertEquals(0, status.episodesPlanned());
        assertEquals(2, status.episodesRemaining());
        assertFalse(status.isOverQuota());
        assertTrue(status.canAddAnotherEpisode());
    }

    @Test
    void countsOneLineAgainstWeekdayLimit() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, List.of(line(false)));

        assertEquals(2, status.episodeLimit());
        assertEquals(1, status.episodesPlanned());
        assertEquals(1, status.episodesRemaining());
        assertFalse(status.isOverQuota());
        assertTrue(status.canAddAnotherEpisode());
    }

    @Test
    void countsCheckedAndUncheckedLinesTheSame() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));
        List<PlanTodayLine> lines = List.of(line(true), line(false));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, lines);

        assertEquals(2, status.episodesPlanned());
        assertEquals(0, status.episodesRemaining());
        assertFalse(status.isOverQuota());
        assertFalse(status.canAddAnotherEpisode());
    }

    @Test
    void appliesWeekendPolicyLimit() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKEND, List.of(line(false)));

        assertEquals(4, status.episodeLimit());
        assertEquals(1, status.episodesPlanned());
        assertEquals(3, status.episodesRemaining());
        assertTrue(status.canAddAnotherEpisode());
    }

    @Test
    void marksDayAsOverQuotaWhenLineCountExceedsLimit() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));
        List<PlanTodayLine> lines = List.of(line(false), line(true), line(false));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, lines);

        assertEquals(3, status.episodesPlanned());
        assertEquals(0, status.episodesRemaining());
        assertTrue(status.isOverQuota());
        assertFalse(status.canAddAnotherEpisode());
    }

    private static LibraryProfile profile(ScreenTimePolicy policy) {
        return LibraryProfile.newProfile(OWNER_ID, "You", policy);
    }

    private static PlanTodayLine line(boolean checked) {
        return new PlanTodayLine(UUID.randomUUID(), "Title", checked, PlanLineSource.MANUAL);
    }
}
