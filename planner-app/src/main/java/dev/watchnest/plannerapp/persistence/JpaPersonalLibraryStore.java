package dev.watchnest.plannerapp.persistence;

import dev.watchnest.planner.domain.ForwardPlanItem;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanToday;
import dev.watchnest.planner.domain.PlanTodayLine;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.plannerapp.library.PersonalLibraryStore;
import dev.watchnest.plannerapp.persistence.jpa.ForwardPlanItemEntity;
import dev.watchnest.plannerapp.persistence.jpa.ForwardPlanItemJpaRepository;
import dev.watchnest.plannerapp.persistence.jpa.LibraryProfileEntity;
import dev.watchnest.plannerapp.persistence.jpa.LibraryProfileJpaRepository;
import dev.watchnest.plannerapp.persistence.jpa.PlanTodayEntity;
import dev.watchnest.plannerapp.persistence.jpa.PlanTodayJpaRepository;
import dev.watchnest.plannerapp.persistence.jpa.PlanTodayLineEntity;
import dev.watchnest.plannerapp.persistence.jpa.PlanTodayLineJpaRepository;
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
    private final PlanTodayJpaRepository planTodays;
    private final PlanTodayLineJpaRepository planTodayLines;
    private final ForwardPlanItemJpaRepository forwardItems;

    public JpaPersonalLibraryStore(
            LibraryProfileJpaRepository profiles,
            WatchEventJpaRepository watchEvents,
            UserAccountJpaRepository userAccounts,
            PlanTodayJpaRepository planTodays,
            PlanTodayLineJpaRepository planTodayLines,
            ForwardPlanItemJpaRepository forwardItems
    ) {
        this.profiles = profiles;
        this.watchEvents = watchEvents;
        this.userAccounts = userAccounts;
        this.planTodays = planTodays;
        this.planTodayLines = planTodayLines;
        this.forwardItems = forwardItems;
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

    @Override
    public Optional<PlanToday> findPlanTodayByOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        return planTodays.findByOwnerId(ownerId).map(this::toDomain);
    }

    @Override
    public void savePlanToday(PlanToday planToday) {
        Objects.requireNonNull(planToday, "planToday");
        PlanTodayEntity entity = planTodays.findByOwnerId(planToday.ownerId())
                .orElseGet(() -> new PlanTodayEntity(UUID.randomUUID(), planToday.ownerId(), planToday.forDate()));
        entity.setForDate(planToday.forDate());
        planTodays.saveAndFlush(entity);
        planTodayLines.deleteByPlanTodayId(entity.getId());
        planTodayLines.flush();
        int sortIndex = 0;
        for (PlanTodayLine line : planToday.lines()) {
            planTodayLines.save(new PlanTodayLineEntity(
                    line.id(),
                    entity.getId(),
                    line.contentTitle(),
                    line.checked(),
                    line.source(),
                    sortIndex++
            ));
        }
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
        return forwardItems.findByOwnerIdAndPlannedForBetweenOrderByPlannedForAscSortIndexAsc(ownerId, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ForwardPlanItem> findForwardPlanItemByOwnerAndId(UUID ownerId, UUID itemId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(itemId, "itemId");
        return forwardItems.findByOwnerIdAndId(ownerId, itemId).map(this::toDomain);
    }

    @Override
    public int countForwardPlanItemsByOwnerAndPlannedFor(UUID ownerId, LocalDate plannedFor) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(plannedFor, "plannedFor");
        return forwardItems.countByOwnerIdAndPlannedFor(ownerId, plannedFor);
    }

    @Override
    public void appendForwardPlanItem(ForwardPlanItem item) {
        Objects.requireNonNull(item, "item");
        Integer maxSortIndex = forwardItems.maxSortIndexByOwnerId(item.ownerId());
        int sortIndex = (maxSortIndex == null ? -1 : maxSortIndex) + 1;
        forwardItems.save(new ForwardPlanItemEntity(
                item.id(),
                item.ownerId(),
                item.plannedFor(),
                item.contentTitle(),
                sortIndex
        ));
    }

    @Override
    public void deleteForwardPlanItem(UUID ownerId, UUID itemId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(itemId, "itemId");
        forwardItems.findByOwnerIdAndId(ownerId, itemId).ifPresent(forwardItems::delete);
    }

    @Override
    public List<ForwardPlanItem> deleteForwardPlanItemsByOwnerAndPlannedForBefore(UUID ownerId, LocalDate date) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(date, "date");
        List<ForwardPlanItemEntity> entities =
                forwardItems.findByOwnerIdAndPlannedForLessThanOrderByPlannedForAscSortIndexAsc(ownerId, date);
        return deleteAll(entities);
    }

    @Override
    public List<ForwardPlanItem> deleteForwardPlanItemsByOwnerAndPlannedFor(UUID ownerId, LocalDate date) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(date, "date");
        List<ForwardPlanItemEntity> entities =
                forwardItems.findByOwnerIdAndPlannedForOrderBySortIndexAsc(ownerId, date);
        return deleteAll(entities);
    }

    private List<ForwardPlanItem> deleteAll(List<ForwardPlanItemEntity> entities) {
        List<ForwardPlanItem> removed = entities.stream().map(this::toDomain).toList();
        forwardItems.deleteAll(entities);
        return removed;
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

    private PlanToday toDomain(PlanTodayEntity entity) {
        List<PlanTodayLine> lines = planTodayLines.findByPlanTodayIdOrderBySortIndexAsc(entity.getId())
                .stream()
                .map(line -> new PlanTodayLine(line.getId(), line.getContentTitle(), line.isChecked(), line.getSource()))
                .toList();
        return new PlanToday(entity.getOwnerId(), entity.getForDate(), lines);
    }

    private ForwardPlanItem toDomain(ForwardPlanItemEntity entity) {
        return new ForwardPlanItem(
                entity.getId(),
                entity.getOwnerId(),
                entity.getPlannedFor(),
                entity.getContentTitle()
        );
    }
}
