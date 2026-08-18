package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateWatchEventRequest(
        @NotNull LocalDate watchedOn,
        @NotNull String contentTitle
) {
}
