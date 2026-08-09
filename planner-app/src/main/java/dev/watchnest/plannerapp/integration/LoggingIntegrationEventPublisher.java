package dev.watchnest.plannerapp.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("memory")
public class LoggingIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingIntegrationEventPublisher.class);

    @Override
    public void publish(PlannerIntegrationEvent event) {
        log.info("planner-integration-event {}", event);
    }
}
