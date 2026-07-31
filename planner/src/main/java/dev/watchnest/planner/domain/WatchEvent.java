package dev.watchnest.planner.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record WatchEvent(UUID id, UUID ownerId, LocalDate watchedOn, String contentTitle) {

    public WatchEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(watchedOn, "watchedOn");
        Objects.requireNonNull(contentTitle, "contentTitle");
        if (contentTitle.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }
    }
}
