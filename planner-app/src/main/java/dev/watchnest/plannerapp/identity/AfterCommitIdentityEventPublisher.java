package dev.watchnest.plannerapp.identity;

import dev.watchnest.identity.port.IdentityEventPublisher;
import dev.watchnest.identity.port.UserRegisteredV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistent")
public class AfterCommitIdentityEventPublisher implements IdentityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitIdentityEventPublisher.class);

    @Override
    public void publish(UserRegisteredV1 event) {
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

    private static void writeLog(UserRegisteredV1 event) {
        log.info("identity-event user-registered userId={} username={}", event.userId(), event.username());
    }
}
