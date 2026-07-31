package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateScreenTimePolicyRequest(
        @Min(0) @Max(20) int weekdayEpisodeLimit,
        @Min(0) @Max(20) int weekendEpisodeLimit
) {
}
