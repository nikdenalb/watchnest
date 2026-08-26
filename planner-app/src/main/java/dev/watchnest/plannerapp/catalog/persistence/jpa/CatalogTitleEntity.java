package dev.watchnest.plannerapp.catalog.persistence.jpa;

import dev.watchnest.catalog.domain.TitleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_title")
public class CatalogTitleEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "title_type", nullable = false, length = 32)
    private TitleType titleType;

    @Column(name = "name_en", nullable = false, length = 255)
    private String nameEn;

    @Column(name = "name_en_key", nullable = false, length = 255)
    private String nameEnKey;

    @Column(name = "name_original", nullable = false, length = 255)
    private String nameOriginal;

    @Column(name = "release_year", nullable = false)
    private int releaseYear;

    @Column(name = "description")
    private String description;

    @Column(name = "genres", length = 1000)
    private String genres;

    @Column(name = "countries", length = 1000)
    private String countries;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CatalogTitleEntity() {
    }

    public CatalogTitleEntity(
            UUID id,
            TitleType titleType,
            String nameEn,
            String nameEnKey,
            String nameOriginal,
            int releaseYear,
            String description,
            String genres,
            String countries,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.titleType = titleType;
        this.nameEn = nameEn;
        this.nameEnKey = nameEnKey;
        this.nameOriginal = nameOriginal;
        this.releaseYear = releaseYear;
        this.description = description;
        this.genres = genres;
        this.countries = countries;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public TitleType getTitleType() {
        return titleType;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameEnKey() {
        return nameEnKey;
    }

    public String getNameOriginal() {
        return nameOriginal;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public String getDescription() {
        return description;
    }

    public String getGenres() {
        return genres;
    }

    public String getCountries() {
        return countries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void replaceWith(
            TitleType titleType,
            String nameEn,
            String nameEnKey,
            String nameOriginal,
            int releaseYear,
            String description,
            String genres,
            String countries,
            Instant updatedAt
    ) {
        this.titleType = titleType;
        this.nameEn = nameEn;
        this.nameEnKey = nameEnKey;
        this.nameOriginal = nameOriginal;
        this.releaseYear = releaseYear;
        this.description = description;
        this.genres = genres;
        this.countries = countries;
        this.updatedAt = updatedAt;
    }
}
