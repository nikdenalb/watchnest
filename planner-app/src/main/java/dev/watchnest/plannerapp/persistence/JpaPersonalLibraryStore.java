package dev.watchnest.plannerapp.persistence;

import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.plannerapp.library.PersonalLibraryStore;
import dev.watchnest.plannerapp.persistence.jpa.LibraryProfileEntity;
import dev.watchnest.plannerapp.persistence.jpa.LibraryProfileJpaRepository;
import dev.watchnest.plannerapp.persistence.jpa.WatchEventEntity;
import dev.watchnest.plannerapp.persistence.jpa.WatchEventJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@Profile("persistent")
public class JpaPersonalLibraryStore implements PersonalLibraryStore {

    private final LibraryProfileJpaRepository profiles;
    private final WatchEventJpaRepository watchEvents;

    public JpaPersonalLibraryStore(
            LibraryProfileJpaRepository profiles,
            WatchEventJpaRepository watchEvents
    ) {
        this.profiles = profiles;
        this.watchEvents = watchEvents;
    }

    @Override
    public LibraryProfile getOrCreateProfile(UUID ownerId, String displayName) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(displayName, "displayName");
        return profiles.findById(ownerId)
                .map(this::toDomain)
                .orElseGet(() -> createProfile(ownerId, displayName));
    }

    @Override
    public void saveProfile(LibraryProfile profile) {
        Objects.requireNonNull(profile, "profile");
        LibraryProfileEntity entity = profiles.findById(profile.id())
                .orElseGet(() -> new LibraryProfileEntity(
                        profile.id(),
                        profile.displayName(),
                        profile.screenTimePolicy().weekdayEpisodeLimit(),
                        profile.screenTimePolicy().weekendEpisodeLimit()
                ));
        entity.setDisplayName(profile.displayName());
        entity.setWeekdayEpisodeLimit(profile.screenTimePolicy().weekdayEpisodeLimit());
        entity.setWeekendEpisodeLimit(profile.screenTimePolicy().weekendEpisodeLimit());
        profiles.save(entity);
    }

    @Override
    public void appendWatchEvent(WatchEvent event) {
        Objects.requireNonNull(event, "event");
        watchEvents.save(new WatchEventEntity(
                event.id(),
                event.ownerId(),
                event.watchedOn(),
                event.contentTitle()
        ));
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
        return watchEvents.findByOwnerIdAndWatchedOnBetweenOrderByWatchedOnDescContentTitleAsc(
                        ownerId,
                        from,
                        to
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    private LibraryProfile createProfile(UUID ownerId, String displayName) {
        LibraryProfileEntity entity = new LibraryProfileEntity(ownerId, displayName, 2, 4);
        try {
            profiles.saveAndFlush(entity);
            return toDomain(entity);
        } catch (DataIntegrityViolationException ex) {
            return profiles.findById(ownerId)
                    .map(this::toDomain)
                    .orElseThrow(() -> ex);
        }
    }

    private LibraryProfile toDomain(LibraryProfileEntity entity) {
        return new LibraryProfile(
                entity.getId(),
                entity.getDisplayName(),
                new ScreenTimePolicy(entity.getWeekdayEpisodeLimit(), entity.getWeekendEpisodeLimit())
        );
    }

    private WatchEvent toDomain(WatchEventEntity entity) {
        return new WatchEvent(
                entity.getId(),
                entity.getOwnerId(),
                entity.getWatchedOn(),
                entity.getContentTitle()
        );
    }
}
