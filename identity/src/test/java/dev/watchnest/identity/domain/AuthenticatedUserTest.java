package dev.watchnest.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void rejectsNullAndBlankUsername() {
        assertThrows(NullPointerException.class, () -> new AuthenticatedUser(null, "alice"));
        assertThrows(NullPointerException.class, () -> new AuthenticatedUser(ID, null));
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatedUser(ID, "  "));
    }
}
