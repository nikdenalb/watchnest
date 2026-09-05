package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface PersonalLibraryStore {

    <T> T withOwnerLock(UUID ownerId, Supplier<T> action);

    void ensureProfile(UUID ownerId, String displayName);

    void appendWatchEvent(WatchEvent event);

    Optional<WatchEvent> findWatchEventByOwnerAndId(UUID ownerId, UUID id);

    int countWatchEventsByOwnerAndWatchedOn(UUID ownerId, LocalDate watchedOn);

    void updateWatchEventTitle(UUID ownerId, UUID id, String trimmedTitle);

    void deleteWatchEvent(UUID ownerId, UUID id);

    List<WatchEvent> findWatchEventsByOwnerAndWatchedOnBetween(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    );
}
