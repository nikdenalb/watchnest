package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentTitleRequest(
        @NotBlank @Size(max = 120) String contentTitle
) {
}
