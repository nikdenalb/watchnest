package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateLibraryPreferencesRequest(
        @NotNull Boolean treatPlanAsWatched
) {
}
