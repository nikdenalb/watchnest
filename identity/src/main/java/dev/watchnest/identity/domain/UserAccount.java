package dev.watchnest.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserAccount(UUID id, Username username, String passwordHash, Instant createdAt) {

    public UserAccount {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, username.value());
    }
}
