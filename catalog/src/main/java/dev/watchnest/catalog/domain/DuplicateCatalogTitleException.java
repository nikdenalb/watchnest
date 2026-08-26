package dev.watchnest.catalog.domain;

import java.util.Objects;

public class DuplicateCatalogTitleException extends RuntimeException {

    private final CatalogTitle existingTitle;

    public DuplicateCatalogTitleException(CatalogTitle existingTitle) {
        super("catalog title already exists: " + Objects.requireNonNull(existingTitle, "existingTitle").id());
        this.existingTitle = existingTitle;
    }

    public CatalogTitle existingTitle() {
        return existingTitle;
    }
}
