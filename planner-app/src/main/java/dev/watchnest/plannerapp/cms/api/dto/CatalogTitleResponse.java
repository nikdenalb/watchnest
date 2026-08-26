package dev.watchnest.plannerapp.cms.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.TitleType;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CatalogTitleResponse(
        UUID id,
        TitleType type,
        String nameEn,
        String nameOriginal,
        int year,
        String description,
        String genres,
        String countries
) {

    public static CatalogTitleResponse from(CatalogTitle title) {
        return new CatalogTitleResponse(
                title.id(),
                title.type(),
                title.nameEn(),
                title.nameOriginal(),
                title.year(),
                title.description(),
                title.genres(),
                title.countries()
        );
    }
}
