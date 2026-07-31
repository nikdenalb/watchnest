package dev.watchnest.planner.domain;

import java.util.Objects;
import java.util.UUID;

public record LibraryProfile(UUID id, String displayName, ScreenTimePolicy screenTimePolicy) {

    public LibraryProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(screenTimePolicy, "screenTimePolicy");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
