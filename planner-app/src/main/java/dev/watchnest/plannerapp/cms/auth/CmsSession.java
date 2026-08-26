package dev.watchnest.plannerapp.cms.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CmsSession(UUID accountId, String username, Instant lastAccessedAt) {

    public CmsSession {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(lastAccessedAt, "lastAccessedAt");
    }
}
