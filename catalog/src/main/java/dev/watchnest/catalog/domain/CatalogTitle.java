package dev.watchnest.catalog.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record CatalogTitle(
        UUID id,
        TitleType type,
        String nameEn,
        String nameOriginal,
        int year,
        String description,
        String genres,
        String countries
) {

    public static final int MIN_YEAR = 1000;
    public static final int MAX_YEAR = 9999;
    public static final int MAX_NAME_LENGTH = 255;
    public static final int MAX_DESCRIPTION_LENGTH = 10_000;
    public static final int MAX_TAG_LIST_LENGTH = 1_000;

    public CatalogTitle {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        nameEn = requireName(nameEn, "nameEn");
        nameOriginal = requireName(nameOriginal, "nameOriginal");
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new InvalidCatalogTitleException(
                    "year must be between " + MIN_YEAR + " and " + MAX_YEAR
            );
        }
        description = canonicalizeDescription(description);
        genres = canonicalizeTagList(genres, "genres");
        countries = canonicalizeTagList(countries, "countries");
    }

    public String nameEnKey() {
        return nameEn.toLowerCase(Locale.ROOT);
    }

    private static String requireName(String raw, String field) {
        if (raw == null) {
            throw new InvalidCatalogTitleException(field + " must not be null");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidCatalogTitleException(field + " must not be blank");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidCatalogTitleException(
                    field + " length must be between 1 and " + MAX_NAME_LENGTH
            );
        }
        return trimmed;
    }

    private static String canonicalizeDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidCatalogTitleException(
                    "description must be at most " + MAX_DESCRIPTION_LENGTH + " characters"
            );
        }
        return trimmed;
    }

    private static String canonicalizeTagList(String raw, String field) {
        if (raw == null) {
            return null;
        }
        List<String> tokens = new ArrayList<>();
        for (String part : raw.split(",", -1)) {
            String token = part.trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return null;
        }
        String canonical = String.join(", ", tokens);
        if (canonical.length() > MAX_TAG_LIST_LENGTH) {
            throw new InvalidCatalogTitleException(
                    field + " must be at most " + MAX_TAG_LIST_LENGTH + " characters"
            );
        }
        return canonical;
    }
}
