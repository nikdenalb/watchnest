package dev.watchnest.plannerapp.cms.api.dto;

import dev.watchnest.catalog.domain.TitleType;
import jakarta.validation.constraints.NotNull;

public record CatalogTitleRequest(
        @NotNull TitleType type,
        @NotNull String nameEn,
        @NotNull String nameOriginal,
        @NotNull Integer year,
        String description,
        String genres,
        String countries
) {
}
