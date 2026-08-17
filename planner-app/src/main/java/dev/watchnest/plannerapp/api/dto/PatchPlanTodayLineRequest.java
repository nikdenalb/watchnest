package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.NotNull;

public record PatchPlanTodayLineRequest(
        @NotNull Boolean checked
) {
}
