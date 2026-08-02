package dev.watchnest.identity.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsernameTest {

    @Test
    void parsesAndCanonicalizes() {
        assertEquals("alice", Username.parse("  Alice  ").value());
        assertEquals("bob.user_1-x", Username.parse("Bob.User_1-X").value());
    }

    @Test
    void acceptsBoundaryLengths() {
        assertEquals("abc", Username.parse("abc").value());
        assertEquals("a".repeat(32), Username.parse("A".repeat(32)).value());
    }

    @Test
    void rejectsNullBlankAndBounds() {
        assertThrows(InvalidUsernameException.class, () -> Username.parse(null));
        assertThrows(InvalidUsernameException.class, () -> Username.parse("  "));
        assertThrows(InvalidUsernameException.class, () -> Username.parse("ab"));
        assertThrows(InvalidUsernameException.class, () -> Username.parse("a".repeat(33)));
    }

    @Test
    void rejectsDisallowedCharacters() {
        assertThrows(InvalidUsernameException.class, () -> Username.parse("alice!"));
        assertThrows(InvalidUsernameException.class, () -> Username.parse("алиса"));
        assertThrows(InvalidUsernameException.class, () -> Username.parse("alice space"));
    }
}
