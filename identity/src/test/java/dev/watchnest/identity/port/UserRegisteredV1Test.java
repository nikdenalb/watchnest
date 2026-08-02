package dev.watchnest.identity.port;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRegisteredV1Test {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Instant AT = Instant.parse("2026-08-02T15:00:00Z");

    @Test
    void rejectsNullFieldsAndBlankUsername() {
        assertThrows(NullPointerException.class, () -> new UserRegisteredV1(null, "alice", AT));
        assertThrows(NullPointerException.class, () -> new UserRegisteredV1(ID, null, AT));
        assertThrows(NullPointerException.class, () -> new UserRegisteredV1(ID, "alice", null));
        assertThrows(IllegalArgumentException.class, () -> new UserRegisteredV1(ID, "  ", AT));
    }
}
