package dev.watchnest.planner.policy;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public final class ScreenTimeQuotaCalculator {

    public DailyScreenTimeStatus summarize(
            LibraryProfile profile,
            LocalDate date,
            Collection<WatchEvent> watchEvents
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(watchEvents, "watchEvents");

        int episodeLimit = profile.screenTimePolicy().episodeLimitFor(date);
        int episodesWatched = countEpisodesWatched(profile.id(), date, watchEvents);
        int episodesRemaining = Math.max(0, episodeLimit - episodesWatched);

        return new DailyScreenTimeStatus(date, episodeLimit, episodesWatched, episodesRemaining);
    }

    private static int countEpisodesWatched(
            UUID ownerId,
            LocalDate date,
            Collection<WatchEvent> watchEvents
    ) {
        int count = 0;
        for (WatchEvent event : watchEvents) {
            if (event.ownerId().equals(ownerId) && event.watchedOn().equals(date)) {
                count++;
            }
        }
        return count;
    }
}
