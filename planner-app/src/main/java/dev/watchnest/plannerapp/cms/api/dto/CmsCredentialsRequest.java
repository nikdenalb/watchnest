package dev.watchnest.plannerapp.cms.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CmsCredentialsRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
