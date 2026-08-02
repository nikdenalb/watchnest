package dev.watchnest.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Username {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 32;
    private static final Pattern ALLOWED = Pattern.compile("^[a-z0-9._-]+$");

    private final String value;

    private Username(String value) {
        this.value = value;
    }

    public static Username parse(String raw) {
        if (raw == null) {
            throw new InvalidUsernameException("username must not be null");
        }
        String canonical = raw.trim().toLowerCase(Locale.ROOT);
        if (canonical.length() < MIN_LENGTH || canonical.length() > MAX_LENGTH) {
            throw new InvalidUsernameException(
                    "username length must be between " + MIN_LENGTH + " and " + MAX_LENGTH
            );
        }
        if (!ALLOWED.matcher(canonical).matches()) {
            throw new InvalidUsernameException(
                    "username may contain only ASCII letters, digits, '.', '_' and '-'"
            );
        }
        return new Username(canonical);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Username username)) {
            return false;
        }
        return value.equals(username.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
