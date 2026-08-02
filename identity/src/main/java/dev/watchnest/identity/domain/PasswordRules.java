package dev.watchnest.identity.domain;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class PasswordRules {

    static final int MIN_CODE_POINTS = 8;
    static final int MAX_UTF8_BYTES = 72;

    private PasswordRules() {
    }

    public static void requireValid(String rawPassword) {
        Objects.requireNonNull(rawPassword, "password");
        int codePoints = rawPassword.codePointCount(0, rawPassword.length());
        if (codePoints < MIN_CODE_POINTS) {
            throw new InvalidPasswordException(
                    "password must contain at least " + MIN_CODE_POINTS + " characters"
            );
        }
        int utf8Bytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        if (utf8Bytes > MAX_UTF8_BYTES) {
            throw new InvalidPasswordException(
                    "password must not exceed " + MAX_UTF8_BYTES + " UTF-8 bytes"
            );
        }
    }
}
