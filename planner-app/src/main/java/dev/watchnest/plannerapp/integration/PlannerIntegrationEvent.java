package dev.watchnest.plannerapp.integration;

import dev.watchnest.planner.domain.ForwardPlanItem;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.UUID;

public sealed interface PlannerIntegrationEvent
        permits PlannerIntegrationEvent.ScreenTimePolicyUpdated,
        PlannerIntegrationEvent.WatchEventRecorded,
        PlannerIntegrationEvent.WatchEventCorrected,
        PlannerIntegrationEvent.WatchEventDeleted,
        PlannerIntegrationEvent.PlanTodayRolled,
        PlannerIntegrationEvent.ForwardPlanItemAdded,
        PlannerIntegrationEvent.ForwardPlanItemRemoved {

    enum ForwardPlanItemRemovalReason {
        USER_DELETED,
        MOVED_TO_TODAY,
        EXPIRED
    }

    record WatchEventRecorded(WatchEvent watchEvent) implements PlannerIntegrationEvent {
    }

    record WatchEventCorrected(String previousTitle, WatchEvent updated) implements PlannerIntegrationEvent {
    }

    record WatchEventDeleted(
            UUID ownerId,
            UUID id,
            LocalDate watchedOn,
            String contentTitle
    ) implements PlannerIntegrationEvent {
    }

    record ScreenTimePolicyUpdated(UUID ownerId, ScreenTimePolicy policy) implements PlannerIntegrationEvent {
    }

    record PlanTodayRolled(UUID ownerId, LocalDate closedDate, int flushedCount) implements PlannerIntegrationEvent {
    }

    record ForwardPlanItemAdded(ForwardPlanItem item) implements PlannerIntegrationEvent {
    }

    record ForwardPlanItemRemoved(
            UUID ownerId,
            UUID itemId,
            LocalDate plannedFor,
            String contentTitle,
            ForwardPlanItemRemovalReason reason
    ) implements PlannerIntegrationEvent {
    }
}
