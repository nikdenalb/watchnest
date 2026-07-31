package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.ScreenTimePolicy;

public record ScreenTimePolicyResponse(int weekdayEpisodeLimit, int weekendEpisodeLimit) {

    public static ScreenTimePolicyResponse from(ScreenTimePolicy policy) {
        return new ScreenTimePolicyResponse(policy.weekdayEpisodeLimit(), policy.weekendEpisodeLimit());
    }
}
