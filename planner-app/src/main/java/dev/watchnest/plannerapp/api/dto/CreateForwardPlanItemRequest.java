package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateForwardPlanItemRequest(
        @NotNull LocalDate plannedFor,
        @NotBlank @Size(max = 120) String contentTitle
) {
}
