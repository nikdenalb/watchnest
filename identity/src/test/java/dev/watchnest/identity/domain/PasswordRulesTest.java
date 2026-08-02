package dev.watchnest.identity.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordRulesTest {

    @Test
    void rejectsNullPassword() {
        assertThrows(NullPointerException.class, () -> PasswordRules.requireValid(null));
    }

    @Test
    void acceptsMinimumCodePointsAndMaximumAsciiBytes() {
        assertDoesNotThrow(() -> PasswordRules.requireValid("a".repeat(8)));
        assertDoesNotThrow(() -> PasswordRules.requireValid("a".repeat(72)));
    }
}
