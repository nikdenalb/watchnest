package dev.watchnest.plannerapp.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistent")
public class AfterCommitIntegrationEventPublisher implements IntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitIntegrationEventPublisher.class);

    @Override
    public void publish(PlannerIntegrationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            writeLog(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                writeLog(event);
            }
        });
    }

    private static void writeLog(PlannerIntegrationEvent event) {
        log.info("planner-integration-event {}", event);
    }
}
