package dev.watchnest.plannerapp.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthCredentialsRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
