package dev.watchnest.plannerapp.api.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String username) {
}
