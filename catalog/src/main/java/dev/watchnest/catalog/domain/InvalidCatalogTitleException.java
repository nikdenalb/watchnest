package dev.watchnest.catalog.domain;

public class InvalidCatalogTitleException extends RuntimeException {

    public InvalidCatalogTitleException(String message) {
        super(message);
    }
}
