package dev.watchnest.plannerapp.integration;

import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;

import java.util.UUID;

public sealed interface PlannerIntegrationEvent
        permits PlannerIntegrationEvent.ScreenTimePolicyUpdated, PlannerIntegrationEvent.WatchEventRecorded {

    record WatchEventRecorded(WatchEvent watchEvent) implements PlannerIntegrationEvent {
    }

    record ScreenTimePolicyUpdated(UUID ownerId, ScreenTimePolicy policy) implements PlannerIntegrationEvent {
    }
}
