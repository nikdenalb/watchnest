package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;

import java.time.LocalDate;

public record DailyScreenTimeStatusResponse(
        LocalDate date,
        int episodeLimit,
        int episodesPlanned,
        int episodesRemaining,
        boolean overQuota,
        boolean canAddAnotherEpisode
) {

    public static DailyScreenTimeStatusResponse from(DailyScreenTimeStatus status) {
        return new DailyScreenTimeStatusResponse(
                status.date(),
                status.episodeLimit(),
                status.episodesPlanned(),
                status.episodesRemaining(),
                status.isOverQuota(),
                status.canAddAnotherEpisode()
        );
    }
}
