package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.ForwardPlanItem;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanToday;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@Profile("memory")
public class InMemoryPersonalLibraryStore implements PersonalLibraryStore {

    private final ConcurrentHashMap<UUID, LibraryProfile> profiles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<WatchEvent>> watchEventsByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PlanToday> planTodayByOwner = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<StoredForwardItem>> forwardByOwner = new ConcurrentHashMap<>();
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
        profiles.put(profile.id(), profile);
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
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        return events.stream()
                .filter(event -> !event.watchedOn().isBefore(from) && !event.watchedOn().isAfter(to))
                .sorted(Comparator.comparing(WatchEvent::watchedOn).reversed()
                        .thenComparing(WatchEvent::contentTitle))
                .toList();
    }

    @Override
    public Optional<PlanToday> findPlanTodayByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        PlanToday plan = planTodayByOwner.get(ownerId);
        if (plan == null) {
            return Optional.empty();
        }
        return Optional.of(new PlanToday(plan.ownerId(), plan.forDate(), List.copyOf(plan.lines())));
    }

    @Override
    public void savePlanToday(PlanToday planToday) {
        Objects.requireNonNull(planToday, "planToday");
        planTodayByOwner.put(
                planToday.ownerId(),
                new PlanToday(planToday.ownerId(), planToday.forDate(), List.copyOf(planToday.lines()))
        );
    }

    @Override
    public List<ForwardPlanItem> findForwardPlanItemsByOwnerAndPlannedForBetween(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return storedForward(ownerId).stream()
                .filter(stored -> !stored.item().plannedFor().isBefore(from)
                        && !stored.item().plannedFor().isAfter(to))
                .sorted(Comparator.comparing((StoredForwardItem stored) -> stored.item().plannedFor())
                        .thenComparingInt(StoredForwardItem::sortIndex))
                .map(StoredForwardItem::item)
                .toList();
    }

    @Override
    public Optional<ForwardPlanItem> findForwardPlanItemByOwnerAndId(UUID ownerId, UUID itemId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(itemId, "itemId");
        return storedForward(ownerId).stream()
                .map(StoredForwardItem::item)
                .filter(item -> item.id().equals(itemId))
                .findFirst();
    }

    @Override
    public int countForwardPlanItemsByOwnerAndPlannedFor(UUID ownerId, LocalDate plannedFor) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(plannedFor, "plannedFor");
        return (int) storedForward(ownerId).stream()
                .filter(stored -> stored.item().plannedFor().equals(plannedFor))
                .count();
    }

    @Override
    public void appendForwardPlanItem(ForwardPlanItem item) {
        Objects.requireNonNull(item, "item");
        List<StoredForwardItem> items = forwardByOwner.computeIfAbsent(item.ownerId(), ignored -> new ArrayList<>());
        int sortIndex = items.stream().mapToInt(StoredForwardItem::sortIndex).max().orElse(-1) + 1;
        items.add(new StoredForwardItem(item, sortIndex));
    }

    @Override
    public void deleteForwardPlanItem(UUID ownerId, UUID itemId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(itemId, "itemId");
        storedForward(ownerId).removeIf(stored -> stored.item().id().equals(itemId));
    }

    @Override
    public List<ForwardPlanItem> deleteForwardPlanItemsByOwnerAndPlannedForBefore(UUID ownerId, LocalDate date) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(date, "date");
        return removeMatching(ownerId, stored -> stored.item().plannedFor().isBefore(date));
    }

    @Override
    public List<ForwardPlanItem> deleteForwardPlanItemsByOwnerAndPlannedFor(UUID ownerId, LocalDate date) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(date, "date");
        return removeMatching(ownerId, stored -> stored.item().plannedFor().equals(date));
    }

    private List<ForwardPlanItem> removeMatching(
            UUID ownerId,
            java.util.function.Predicate<StoredForwardItem> predicate
    ) {
        List<StoredForwardItem> items = storedForward(ownerId);
        List<ForwardPlanItem> removed = items.stream()
                .filter(predicate)
                .sorted(Comparator.comparing((StoredForwardItem stored) -> stored.item().plannedFor())
                        .thenComparingInt(StoredForwardItem::sortIndex))
                .map(StoredForwardItem::item)
                .toList();
        items.removeIf(predicate);
        return removed;
    }

    private List<WatchEvent> ownerEvents(UUID ownerId) {
        return watchEventsByOwner.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
    }

    private List<StoredForwardItem> storedForward(UUID ownerId) {
        return forwardByOwner.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
    }

    private Object lockFor(UUID ownerId) {
        return locks.computeIfAbsent(ownerId, ignored -> new Object());
    }

    private record StoredForwardItem(ForwardPlanItem item, int sortIndex) {
    }
}
