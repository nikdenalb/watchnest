package dev.watchnest.identity.domain;

import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String username) {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
