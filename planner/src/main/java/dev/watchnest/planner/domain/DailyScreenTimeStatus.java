package dev.watchnest.planner.domain;

import java.time.LocalDate;
import java.util.Objects;

public record DailyScreenTimeStatus(
        LocalDate date,
        int episodeLimit,
        int episodesPlanned,
        int episodesRemaining
) {

    public DailyScreenTimeStatus {
        Objects.requireNonNull(date, "date");
        if (episodeLimit < 0) {
            throw new IllegalArgumentException("episodeLimit must be non-negative");
        }
        if (episodesPlanned < 0) {
            throw new IllegalArgumentException("episodesPlanned must be non-negative");
        }
        if (episodesRemaining < 0) {
            throw new IllegalArgumentException("episodesRemaining must be non-negative");
        }
    }

    public boolean isOverQuota() {
        return episodesPlanned > episodeLimit;
    }

    public boolean canAddAnotherEpisode() {
        return episodesPlanned < episodeLimit;
    }
}
