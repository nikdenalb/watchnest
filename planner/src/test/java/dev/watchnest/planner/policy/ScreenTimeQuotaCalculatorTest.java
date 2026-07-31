package dev.watchnest.planner.policy;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenTimeQuotaCalculatorTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 7, 6);
    private static final LocalDate WEEKEND = LocalDate.of(2026, 7, 4);

    private final ScreenTimeQuotaCalculator calculator = new ScreenTimeQuotaCalculator();

    @Test
    void summarizesEmptyDayWithFullRemainingQuota() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, List.of());

        assertEquals(WEEKDAY, status.date());
        assertEquals(2, status.episodeLimit());
        assertEquals(0, status.episodesWatched());
        assertEquals(2, status.episodesRemaining());
        assertFalse(status.isOverQuota());
        assertTrue(status.canWatchAnotherEpisode());
    }

    @Test
    void countsOnlyMatchingOwnerAndDate() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));
        List<WatchEvent> events = List.of(
                event(OWNER_ID, WEEKDAY, "Episode 1"),
                event(OWNER_ID, WEEKDAY, "Episode 2"),
                event(OTHER_OWNER_ID, WEEKDAY, "Other owner"),
                event(OWNER_ID, WEEKDAY.plusDays(1), "Next day")
        );

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, events);

        assertEquals(2, status.episodesWatched());
        assertEquals(0, status.episodesRemaining());
        assertFalse(status.isOverQuota());
        assertFalse(status.canWatchAnotherEpisode());
    }

    @Test
    void appliesWeekendPolicyLimit() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKEND, List.of(
                event(OWNER_ID, WEEKEND, "Saturday morning")
        ));

        assertEquals(4, status.episodeLimit());
        assertEquals(1, status.episodesWatched());
        assertEquals(3, status.episodesRemaining());
    }

    @Test
    void marksDayAsOverQuotaWhenWatchLogExceedsLimit() {
        LibraryProfile profile = profile(new ScreenTimePolicy(2, 4));
        List<WatchEvent> events = List.of(
                event(OWNER_ID, WEEKDAY, "Episode 1"),
                event(OWNER_ID, WEEKDAY, "Episode 2"),
                event(OWNER_ID, WEEKDAY, "Off-platform TV")
        );

        DailyScreenTimeStatus status = calculator.summarize(profile, WEEKDAY, events);

        assertEquals(3, status.episodesWatched());
        assertEquals(0, status.episodesRemaining());
        assertTrue(status.isOverQuota());
        assertFalse(status.canWatchAnotherEpisode());
    }

    private static LibraryProfile profile(ScreenTimePolicy policy) {
        return new LibraryProfile(OWNER_ID, "You", policy);
    }

    private static WatchEvent event(UUID ownerId, LocalDate watchedOn, String title) {
        return new WatchEvent(UUID.randomUUID(), ownerId, watchedOn, title);
    }
}
