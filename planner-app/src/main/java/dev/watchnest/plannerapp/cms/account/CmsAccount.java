package dev.watchnest.plannerapp.cms.account;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CmsAccount(UUID id, String username, String passwordHash, Instant createdAt) {

    public CmsAccount {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
