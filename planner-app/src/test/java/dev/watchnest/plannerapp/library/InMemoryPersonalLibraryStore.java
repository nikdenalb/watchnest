package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class InMemoryPersonalLibraryStore implements PersonalLibraryStore {

    private final ConcurrentHashMap<UUID, String> profiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<WatchEvent>> watchEventsByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withOwnerLock(UUID ownerId, Supplier<T> action) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(action, "action");
        synchronized (lockFor(ownerId)) {
            return action.get();
        }
    }

    @Override
    public void ensureProfile(UUID ownerId, String displayName) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(displayName, "displayName");
        profiles.putIfAbsent(ownerId, displayName);
    }

    @Override
    public void appendWatchEvent(WatchEvent event) {
        Objects.requireNonNull(event, "event");
        ownerEvents(event.ownerId()).add(event);
    }

    @Override
    public Optional<WatchEvent> findWatchEventByOwnerAndId(UUID ownerId, UUID id) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        return ownerEvents(ownerId).stream()
                .filter(event -> event.id().equals(id))
                .findFirst();
    }

    @Override
    public int countWatchEventsByOwnerAndWatchedOn(UUID ownerId, LocalDate watchedOn) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(watchedOn, "watchedOn");
        return (int) ownerEvents(ownerId).stream()
                .filter(event -> event.watchedOn().equals(watchedOn))
                .count();
    }

    @Override
    public void updateWatchEventTitle(UUID ownerId, UUID id, String trimmedTitle) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(trimmedTitle, "trimmedTitle");
        List<WatchEvent> events = ownerEvents(ownerId);
        for (int index = 0; index < events.size(); index++) {
            WatchEvent current = events.get(index);
            if (current.id().equals(id)) {
                events.set(index, new WatchEvent(
                        current.id(),
                        current.ownerId(),
                        current.watchedOn(),
                        trimmedTitle
                ));
                return;
            }
        }
    }

    @Override
    public void deleteWatchEvent(UUID ownerId, UUID id) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        ownerEvents(ownerId).removeIf(event -> event.id().equals(id));
    }

    @Override
    public List<WatchEvent> findWatchEventsByOwnerAndWatchedOnBetween(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        List<WatchEvent> events = watchEventsByOwner.get(ownerId);
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .filter(event -> !event.watchedOn().isBefore(from) && !event.watchedOn().isAfter(to))
                .sorted(Comparator.comparing(WatchEvent::watchedOn).reversed()
                        .thenComparing(WatchEvent::contentTitle))
                .toList();
    }

    private List<WatchEvent> ownerEvents(UUID ownerId) {
        return watchEventsByOwner.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
    }

    private Object lockFor(UUID ownerId) {
        return locks.computeIfAbsent(ownerId, ignored -> new Object());
    }
}
