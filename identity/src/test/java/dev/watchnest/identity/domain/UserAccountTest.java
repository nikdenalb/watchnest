package dev.watchnest.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAccountTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Instant CREATED = Instant.parse("2026-08-02T15:00:00Z");

    @Test
    void rejectsNullFieldsAndBlankHash() {
        Username username = Username.parse("alice");
        assertThrows(NullPointerException.class, () -> new UserAccount(null, username, "hash", CREATED));
        assertThrows(NullPointerException.class, () -> new UserAccount(ID, null, "hash", CREATED));
        assertThrows(NullPointerException.class, () -> new UserAccount(ID, username, null, CREATED));
        assertThrows(NullPointerException.class, () -> new UserAccount(ID, username, "hash", null));
        assertThrows(IllegalArgumentException.class, () -> new UserAccount(ID, username, "  ", CREATED));
    }

    @Test
    void toAuthenticatedUserUsesCanonicalUsername() {
        UserAccount account = new UserAccount(ID, Username.parse("Alice"), "hash", CREATED);
        AuthenticatedUser user = account.toAuthenticatedUser();
        assertEquals(ID, user.id());
        assertEquals("alice", user.username());
    }
}
