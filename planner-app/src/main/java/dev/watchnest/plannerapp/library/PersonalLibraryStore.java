package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.WatchEvent;

import java.util.List;
import java.util.UUID;

public interface PersonalLibraryStore {

    LibraryProfile getOrCreateProfile(UUID ownerId, String displayName);

    void saveProfile(LibraryProfile profile);

    void appendWatchEvent(WatchEvent event);

    List<WatchEvent> findWatchEventsByOwner(UUID ownerId);
}
