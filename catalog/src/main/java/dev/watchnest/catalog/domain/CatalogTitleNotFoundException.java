package dev.watchnest.catalog.domain;

import java.util.Objects;
import java.util.UUID;

public class CatalogTitleNotFoundException extends RuntimeException {

    private final UUID titleId;

    public CatalogTitleNotFoundException(UUID titleId) {
        super("catalog title not found: " + titleId);
        this.titleId = Objects.requireNonNull(titleId, "titleId");
    }

    public UUID titleId() {
        return titleId;
    }
}
