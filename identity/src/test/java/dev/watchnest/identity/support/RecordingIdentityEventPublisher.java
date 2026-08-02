package dev.watchnest.identity.support;

import dev.watchnest.identity.port.IdentityEventPublisher;
import dev.watchnest.identity.port.UserRegisteredV1;

import java.util.ArrayList;
import java.util.List;

public final class RecordingIdentityEventPublisher implements IdentityEventPublisher {

    private final List<UserRegisteredV1> events = new ArrayList<>();

    @Override
    public void publish(UserRegisteredV1 event) {
        events.add(event);
    }

    public List<UserRegisteredV1> events() {
        return List.copyOf(events);
    }
}
