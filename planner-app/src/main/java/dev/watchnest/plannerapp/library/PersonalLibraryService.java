package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.LibraryLimits;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.plannerapp.api.dto.WatchEventArchiveResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventResponse;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Transactional
public class PersonalLibraryService {

    public static final int MAX_RANGE_DAYS = 366;
    public static final int MAX_TITLE_LENGTH = 120;

    private final IntegrationEventPublisher integrationEventPublisher;
    private final PersonalLibraryStore store;

    public PersonalLibraryService(
            IntegrationEventPublisher integrationEventPublisher,
            PersonalLibraryStore store
    ) {
        this.integrationEventPublisher = integrationEventPublisher;
        this.store = store;
    }

    @Transactional(readOnly = true)
    public WatchEventArchiveResponse listWatchEvents(UUID ownerId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(ownerId, "ownerId");
        requireValidInclusiveDateRange(from, to);
        List<WatchEventResponse> events = store.findWatchEventsByOwnerAndWatchedOnBetween(ownerId, from, to)
                .stream()
                .map(WatchEventResponse::from)
                .toList();
        return new WatchEventArchiveResponse(from, to, events);
    }

    public WatchEventResponse addWatchEvent(
            UUID ownerId,
            String username,
            LocalDate watchedOn,
            String contentTitle
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        String title = normalizeTitle(contentTitle);
        if (watchedOn == null) {
            throw new IllegalArgumentException("watchedOn is required");
        }
        return inOwnerWrite(ownerId, () -> {
            store.ensureProfile(ownerId, username);
            if (store.countWatchEventsByOwnerAndWatchedOn(ownerId, watchedOn)
                    >= LibraryLimits.MAX_TITLES_PER_DATE) {
                throw new IllegalArgumentException(
                        "a day may have at most " + LibraryLimits.MAX_TITLES_PER_DATE + " watch events"
                );
            }
            WatchEvent event = new WatchEvent(UUID.randomUUID(), ownerId, watchedOn, title);
            store.appendWatchEvent(event);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventRecorded(event));
            return WatchEventResponse.from(event);
        });
    }

    public WatchEventResponse patchWatchEvent(UUID ownerId, UUID id, String contentTitle) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        String title = normalizeTitle(contentTitle);
        return inOwnerWrite(ownerId, () -> {
            WatchEvent current = store.findWatchEventByOwnerAndId(ownerId, id)
                    .orElseThrow(() -> new LibraryResourceNotFoundException("watch event not found"));
            if (title.equals(current.contentTitle())) {
                return WatchEventResponse.from(current);
            }
            store.updateWatchEventTitle(ownerId, id, title);
            WatchEvent updated = new WatchEvent(current.id(), current.ownerId(), current.watchedOn(), title);
            integrationEventPublisher.publish(
                    new PlannerIntegrationEvent.WatchEventCorrected(current.contentTitle(), updated)
            );
            return WatchEventResponse.from(updated);
        });
    }

    public void deleteWatchEvent(UUID ownerId, UUID id) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        inOwnerWrite(ownerId, () -> {
            WatchEvent current = store.findWatchEventByOwnerAndId(ownerId, id)
                    .orElseThrow(() -> new LibraryResourceNotFoundException("watch event not found"));
            store.deleteWatchEvent(ownerId, id);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventDeleted(
                    current.ownerId(),
                    current.id(),
                    current.watchedOn(),
                    current.contentTitle()
            ));
            return null;
        });
    }

    static String normalizeTitle(String contentTitle) {
        if (contentTitle == null) {
            throw new IllegalArgumentException("contentTitle is required");
        }
        String trimmed = contentTitle.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "contentTitle must be at most " + MAX_TITLE_LENGTH + " characters"
            );
        }
        return trimmed;
    }

    static void requireValidInclusiveDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (inclusiveDays > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("date range must be at most 366 days");
        }
    }

    private <T> T inOwnerWrite(UUID ownerId, Supplier<T> action) {
        Objects.requireNonNull(ownerId, "ownerId");
        return store.withOwnerLock(ownerId, action);
    }
}
