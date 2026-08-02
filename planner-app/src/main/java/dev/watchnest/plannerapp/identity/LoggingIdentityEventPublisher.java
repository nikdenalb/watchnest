package dev.watchnest.plannerapp.identity;

import dev.watchnest.identity.port.IdentityEventPublisher;
import dev.watchnest.identity.port.UserRegisteredV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingIdentityEventPublisher implements IdentityEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingIdentityEventPublisher.class);

    @Override
    public void publish(UserRegisteredV1 event) {
        log.info("identity-event user-registered userId={} username={}", event.userId(), event.username());
    }
}
