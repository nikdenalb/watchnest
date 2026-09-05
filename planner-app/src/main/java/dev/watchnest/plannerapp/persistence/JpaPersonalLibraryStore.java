package dev.watchnest.plannerapp.persistence;

import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.plannerapp.library.PersonalLibraryStore;
import dev.watchnest.plannerapp.persistence.jpa.LibraryProfileEntity;
import dev.watchnest.plannerapp.persistence.jpa.LibraryProfileJpaRepository;
import dev.watchnest.plannerapp.persistence.jpa.UserAccountJpaRepository;
import dev.watchnest.plannerapp.persistence.jpa.WatchEventEntity;
import dev.watchnest.plannerapp.persistence.jpa.WatchEventJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@Profile("persistent")
public class JpaPersonalLibraryStore implements PersonalLibraryStore {

    private final LibraryProfileJpaRepository profiles;
    private final WatchEventJpaRepository watchEvents;
    private final UserAccountJpaRepository userAccounts;

    public JpaPersonalLibraryStore(
            LibraryProfileJpaRepository profiles,
            WatchEventJpaRepository watchEvents,
            UserAccountJpaRepository userAccounts
    ) {
        this.profiles = profiles;
        this.watchEvents = watchEvents;
        this.userAccounts = userAccounts;
    }

    @Override
    public <T> T withOwnerLock(UUID ownerId, Supplier<T> action) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(action, "action");
        userAccounts.findByIdForUpdate(ownerId)
                .orElseThrow(() -> new IllegalStateException("missing user_account for owner " + ownerId));
        return action.get();
    }

    @Override
    public void ensureProfile(UUID ownerId, String displayName) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(displayName, "displayName");
        if (profiles.existsById(ownerId)) {
            return;
        }
        try {
            profiles.saveAndFlush(new LibraryProfileEntity(ownerId, displayName));
        } catch (DataIntegrityViolationException ex) {
            if (profiles.existsById(ownerId)) {
                return;
            }
            throw ex;
        }
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
    public Optional<WatchEvent> findWatchEventByOwnerAndId(UUID ownerId, UUID id) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        return watchEvents.findByOwnerIdAndId(ownerId, id).map(this::toDomain);
    }

    @Override
    public int countWatchEventsByOwnerAndWatchedOn(UUID ownerId, LocalDate watchedOn) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(watchedOn, "watchedOn");
        return Math.toIntExact(watchEvents.countByOwnerIdAndWatchedOn(ownerId, watchedOn));
    }

    @Override
    public void updateWatchEventTitle(UUID ownerId, UUID id, String trimmedTitle) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(trimmedTitle, "trimmedTitle");
        watchEvents.findByOwnerIdAndId(ownerId, id).ifPresent(entity -> {
            entity.setContentTitle(trimmedTitle);
            watchEvents.save(entity);
        });
    }

    @Override
    public void deleteWatchEvent(UUID ownerId, UUID id) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        watchEvents.findByOwnerIdAndId(ownerId, id).ifPresent(watchEvents::delete);
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

    private WatchEvent toDomain(WatchEventEntity entity) {
        return new WatchEvent(
                entity.getId(),
                entity.getOwnerId(),
                entity.getWatchedOn(),
                entity.getContentTitle()
        );
    }
}
