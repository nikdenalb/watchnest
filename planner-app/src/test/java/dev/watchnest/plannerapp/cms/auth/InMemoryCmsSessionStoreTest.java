package dev.watchnest.plannerapp.cms.auth;

import dev.watchnest.plannerapp.support.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCmsSessionStoreTest {

    private static final Instant T0 = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEMO_ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void expiresAfterThirtyIdleMinutes() {
        MutableClock clock = new MutableClock(T0);
        InMemoryCmsSessionStore store = new InMemoryCmsSessionStore(clock);
        String token = store.create(ACCOUNT, "editor", false);

        clock.advance(Duration.ofMinutes(30).minusSeconds(1));
        assertTrue(store.authenticate(token).isPresent());

        clock.setInstant(T0.plus(Duration.ofMinutes(30)));
        String idle = store.create(ACCOUNT, "other", false);
        clock.advance(InMemoryCmsSessionStore.IDLE_TIMEOUT);
        assertTrue(store.authenticate(idle).isEmpty());
        assertEquals(0, store.size());
    }

    @Test
    void revokeRemovesOnlyThatToken() {
        MutableClock clock = new MutableClock(T0);
        InMemoryCmsSessionStore store = new InMemoryCmsSessionStore(clock);
        String first = store.create(ACCOUNT, "editor", false);
        String second = store.create(ACCOUNT, "editor", false);

        store.revoke(first);
        assertTrue(store.authenticate(first).isEmpty());
        assertEquals(ACCOUNT, store.authenticate(second).orElseThrow().accountId());
    }

    @Test
    void createAndIdleTouchPreserveDemoSnapshot() {
        MutableClock clock = new MutableClock(T0);
        InMemoryCmsSessionStore store = new InMemoryCmsSessionStore(clock);
        String demoToken = store.create(DEMO_ACCOUNT, "demo", true);
        String editorToken = store.create(ACCOUNT, "editor", false);

        CmsSession demo = store.authenticate(demoToken).orElseThrow();
        CmsSession editor = store.authenticate(editorToken).orElseThrow();
        assertTrue(demo.demo());
        assertEquals(DEMO_ACCOUNT, demo.accountId());
        assertEquals("demo", demo.username());
        assertFalse(editor.demo());
        assertEquals("editor", editor.username());

        clock.advance(Duration.ofMinutes(5));
        CmsSession demoTouched = store.authenticate(demoToken).orElseThrow();
        CmsSession editorTouched = store.authenticate(editorToken).orElseThrow();
        assertTrue(demoTouched.demo());
        assertEquals(DEMO_ACCOUNT, demoTouched.accountId());
        assertEquals("demo", demoTouched.username());
        assertFalse(editorTouched.demo());
        assertEquals("editor", editorTouched.username());
    }
}
