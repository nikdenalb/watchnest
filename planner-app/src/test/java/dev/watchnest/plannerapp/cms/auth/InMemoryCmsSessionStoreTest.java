package dev.watchnest.plannerapp.cms.auth;

import dev.watchnest.plannerapp.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCmsSessionStoreTest {

    private static final Instant T0 = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void expiresAfterThirtyIdleMinutes() {
        MutableClock clock = new MutableClock(T0);
        InMemoryCmsSessionStore store = new InMemoryCmsSessionStore(clock);
        String token = store.create(ACCOUNT, "editor");

        clock.advance(Duration.ofMinutes(30).minusSeconds(1));
        assertTrue(store.authenticate(token).isPresent());

        clock.setInstant(T0.plus(Duration.ofMinutes(30)));
        String idle = store.create(ACCOUNT, "other");
        clock.advance(InMemoryCmsSessionStore.IDLE_TIMEOUT);
        assertTrue(store.authenticate(idle).isEmpty());
        assertEquals(0, store.size());
    }

    @Test
    void revokeRemovesOnlyThatToken() {
        MutableClock clock = new MutableClock(T0);
        InMemoryCmsSessionStore store = new InMemoryCmsSessionStore(clock);
        String first = store.create(ACCOUNT, "editor");
        String second = store.create(ACCOUNT, "editor");

        store.revoke(first);
        assertTrue(store.authenticate(first).isEmpty());
        assertEquals(ACCOUNT, store.authenticate(second).orElseThrow().accountId());
    }
}
