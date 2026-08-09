package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("memory")
public class InMemoryPersonalLibraryStore implements PersonalLibraryStore {

    private final ConcurrentHashMap<UUID, LibraryProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<WatchEvent>> watchEventsByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> locks = new ConcurrentHashMap<>();

    @Override
    public LibraryProfile getOrCreateProfile(UUID ownerId, String displayName) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(displayName, "displayName");
        return profiles.computeIfAbsent(
                ownerId,
                id -> new LibraryProfile(id, displayName, new ScreenTimePolicy(2, 4))
        );
    }

    @Override
    public void saveProfile(LibraryProfile profile) {
        Objects.requireNonNull(profile, "profile");
        synchronized (lockFor(profile.id())) {
            profiles.put(profile.id(), profile);
        }
    }

    @Override
    public void appendWatchEvent(WatchEvent event) {
        Objects.requireNonNull(event, "event");
        synchronized (lockFor(event.ownerId())) {
            List<WatchEvent> events = watchEventsByOwner.computeIfAbsent(
                    event.ownerId(),
                    ignored -> new ArrayList<>()
            );
            events.add(event);
        }
    }

    @Override
    public List<WatchEvent> findWatchEventsByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        synchronized (lockFor(ownerId)) {
            List<WatchEvent> events = watchEventsByOwner.get(ownerId);
            if (events == null || events.isEmpty()) {
                return List.of();
            }
            return List.copyOf(events);
        }
    }

    private Object lockFor(UUID ownerId) {
        return locks.computeIfAbsent(ownerId, ignored -> new Object());
    }
}
