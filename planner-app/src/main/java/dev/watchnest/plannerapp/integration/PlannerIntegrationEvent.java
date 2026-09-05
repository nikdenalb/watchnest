package dev.watchnest.plannerapp.integration;

import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.UUID;

public sealed interface PlannerIntegrationEvent
        permits PlannerIntegrationEvent.WatchEventRecorded,
        PlannerIntegrationEvent.WatchEventCorrected,
        PlannerIntegrationEvent.WatchEventDeleted {

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
}
