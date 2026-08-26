package dev.watchnest.plannerapp.cms.api.dto;

public record DuplicateCatalogTitleResponse(
        String code,
        String message,
        CatalogTitleResponse existingTitle
) {
}
