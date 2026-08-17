package dev.watchnest.planner.domain;

import java.util.Objects;

final class ContentTitles {

    static final int MAX_LENGTH = 120;

    private ContentTitles() {
    }

    static String requireValid(String contentTitle) {
        Objects.requireNonNull(contentTitle, "contentTitle");
        String trimmed = contentTitle.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "contentTitle must be at most " + MAX_LENGTH + " characters"
            );
        }
        return trimmed;
    }
}
