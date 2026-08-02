package dev.watchnest.identity.port;

public interface IdentityEventPublisher {

    void publish(UserRegisteredV1 event);
}
