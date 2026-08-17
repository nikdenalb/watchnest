package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.ForwardPlanItem;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanToday;
import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface PersonalLibraryStore {

    <T> T withOwnerLock(UUID ownerId, Supplier<T> action);

    LibraryProfile getOrCreateProfile(UUID ownerId, String displayName);

    void saveProfile(LibraryProfile profile);

    void appendWatchEvent(WatchEvent event);

    List<WatchEvent> findWatchEventsByOwnerAndWatchedOnBetween(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    );

    Optional<PlanToday> findPlanTodayByOwner(UUID ownerId);

    void savePlanToday(PlanToday planToday);

    List<ForwardPlanItem> findForwardPlanItemsByOwnerAndPlannedForBetween(
            UUID ownerId,
            LocalDate from,
            LocalDate to
    );

    Optional<ForwardPlanItem> findForwardPlanItemByOwnerAndId(UUID ownerId, UUID itemId);

    int countForwardPlanItemsByOwnerAndPlannedFor(UUID ownerId, LocalDate plannedFor);

    void appendForwardPlanItem(ForwardPlanItem item);

    void deleteForwardPlanItem(UUID ownerId, UUID itemId);

    List<ForwardPlanItem> deleteForwardPlanItemsByOwnerAndPlannedForBefore(UUID ownerId, LocalDate date);

    List<ForwardPlanItem> deleteForwardPlanItemsByOwnerAndPlannedFor(UUID ownerId, LocalDate date);
}
