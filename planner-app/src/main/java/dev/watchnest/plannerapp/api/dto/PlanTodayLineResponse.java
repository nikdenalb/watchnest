package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.PlanLineSource;
import dev.watchnest.planner.domain.PlanTodayLine;

import java.util.UUID;

public record PlanTodayLineResponse(
        UUID id,
        String contentTitle,
        boolean checked,
        PlanLineSource source
) {

    public static PlanTodayLineResponse from(PlanTodayLine line) {
        return new PlanTodayLineResponse(line.id(), line.contentTitle(), line.checked(), line.source());
    }
}
