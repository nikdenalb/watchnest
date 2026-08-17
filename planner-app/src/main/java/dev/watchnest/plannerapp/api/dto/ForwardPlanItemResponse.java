package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.ForwardPlanItem;

import java.time.LocalDate;
import java.util.UUID;

public record ForwardPlanItemResponse(
        UUID id,
        LocalDate plannedFor,
        String contentTitle
) {

    public static ForwardPlanItemResponse from(ForwardPlanItem item) {
        return new ForwardPlanItemResponse(item.id(), item.plannedFor(), item.contentTitle());
    }
}
