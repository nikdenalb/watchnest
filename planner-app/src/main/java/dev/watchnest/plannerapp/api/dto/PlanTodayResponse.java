package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.PlanToday;

import java.time.LocalDate;
import java.util.List;

public record PlanTodayResponse(
        LocalDate date,
        List<PlanTodayLineResponse> lines
) {

    public static PlanTodayResponse from(PlanToday plan) {
        return new PlanTodayResponse(
                plan.forDate(),
                plan.lines().stream().map(PlanTodayLineResponse::from).toList()
        );
    }
}
