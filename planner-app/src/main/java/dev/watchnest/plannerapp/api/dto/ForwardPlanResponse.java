package dev.watchnest.plannerapp.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ForwardPlanResponse(
        LocalDate from,
        LocalDate to,
        List<ForwardPlanItemResponse> items
) {
}
