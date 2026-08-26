package dev.watchnest.plannerapp.catalog.event;

import dev.watchnest.catalog.port.CatalogIntegrationEvent;
import dev.watchnest.catalog.port.CatalogIntegrationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistent")
public class AfterCommitCatalogIntegrationEventPublisher implements CatalogIntegrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitCatalogIntegrationEventPublisher.class);

    @Override
    public void publish(CatalogIntegrationEvent event) {
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

    private static void writeLog(CatalogIntegrationEvent event) {
        log.info("catalog-integration-event {}", event);
    }
}
