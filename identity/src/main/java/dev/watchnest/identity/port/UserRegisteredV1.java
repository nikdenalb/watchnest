package dev.watchnest.identity.port;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserRegisteredV1(UUID userId, String username, Instant occurredAt) {

    public UserRegisteredV1 {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}
