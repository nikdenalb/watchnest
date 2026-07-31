package dev.watchnest.plannerapp.integration;

@FunctionalInterface
public interface IntegrationEventPublisher {

    void publish(PlannerIntegrationEvent event);
}
