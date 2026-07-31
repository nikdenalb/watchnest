package dev.watchnest.planner.domain;

import java.time.LocalDate;
import java.util.Objects;

public record DailyScreenTimeStatus(
        LocalDate date,
        int episodeLimit,
        int episodesWatched,
        int episodesRemaining
) {

    public DailyScreenTimeStatus {
        Objects.requireNonNull(date, "date");
        if (episodeLimit < 0) {
            throw new IllegalArgumentException("episodeLimit must be non-negative");
        }
        if (episodesWatched < 0) {
            throw new IllegalArgumentException("episodesWatched must be non-negative");
        }
        if (episodesRemaining < 0) {
            throw new IllegalArgumentException("episodesRemaining must be non-negative");
        }
    }

    public boolean isOverQuota() {
        return episodesWatched > episodeLimit;
    }

    public boolean canWatchAnotherEpisode() {
        return episodesWatched < episodeLimit;
    }
}
