package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;

import java.time.LocalDate;

public record DailyScreenTimeStatusResponse(
        LocalDate date,
        int episodeLimit,
        int episodesWatched,
        int episodesRemaining,
        boolean overQuota,
        boolean canWatchAnotherEpisode
) {

    public static DailyScreenTimeStatusResponse from(DailyScreenTimeStatus status) {
        return new DailyScreenTimeStatusResponse(
                status.date(),
                status.episodeLimit(),
                status.episodesWatched(),
                status.episodesRemaining(),
                status.isOverQuota(),
                status.canWatchAnotherEpisode()
        );
    }
}
