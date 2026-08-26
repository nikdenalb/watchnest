package dev.watchnest.plannerapp.catalog.persistence;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.CatalogTitleNotFoundException;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.port.CatalogTitleRepository;
import dev.watchnest.plannerapp.catalog.persistence.jpa.CatalogTitleEntity;
import dev.watchnest.plannerapp.catalog.persistence.jpa.CatalogTitleJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("persistent")
public class JpaCatalogTitleRepository implements CatalogTitleRepository {

    private static final Comparator<CatalogTitle> TITLE_ORDER = Comparator
            .comparing(CatalogTitle::nameEnKey)
            .thenComparingInt(CatalogTitle::year)
            .thenComparing(CatalogTitle::type)
            .thenComparing(CatalogTitle::id);

    private final CatalogTitleJpaRepository jpa;
    private final Clock clock;

    public JpaCatalogTitleRepository(CatalogTitleJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void insert(CatalogTitle title) {
        Objects.requireNonNull(title, "title");
        Instant now = Instant.now(clock);
        jpa.saveAndFlush(toEntity(title, now, now));
    }

    @Override
    public void update(CatalogTitle title) {
        Objects.requireNonNull(title, "title");
        CatalogTitleEntity entity = jpa.findById(title.id())
                .orElseThrow(() -> new CatalogTitleNotFoundException(title.id()));
        entity.replaceWith(
                title.type(),
                title.nameEn(),
                title.nameEnKey(),
                title.nameOriginal(),
                title.year(),
                title.description(),
                title.genres(),
                title.countries(),
                Instant.now(clock)
        );
        jpa.saveAndFlush(entity);
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id");
        if (!jpa.existsById(id)) {
            throw new CatalogTitleNotFoundException(id);
        }
        jpa.deleteById(id);
        jpa.flush();
    }

    @Override
    public Optional<CatalogTitle> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CatalogTitle> findByNaturalKey(String nameEnKey, int year, TitleType type) {
        Objects.requireNonNull(nameEnKey, "nameEnKey");
        Objects.requireNonNull(type, "type");
        return jpa.findByNameEnKeyAndReleaseYearAndTitleType(nameEnKey, year, type).map(this::toDomain);
    }

    @Override
    public List<CatalogTitle> findAllSorted() {
        return jpa.findAll().stream().map(this::toDomain).sorted(TITLE_ORDER).toList();
    }

    @Override
    public List<CatalogTitle> findByNameEnContainingLiteral(String query) {
        String needle = normalizeQuery(query);
        if (needle.isEmpty()) {
            return findAllSorted();
        }
        return jpa.searchLiteral("%" + escapeLike(needle) + "%").stream()
                .map(this::toDomain)
                .sorted(TITLE_ORDER)
                .toList();
    }

    private CatalogTitleEntity toEntity(CatalogTitle title, Instant createdAt, Instant updatedAt) {
        return new CatalogTitleEntity(
                title.id(),
                title.type(),
                title.nameEn(),
                title.nameEnKey(),
                title.nameOriginal(),
                title.year(),
                title.description(),
                title.genres(),
                title.countries(),
                createdAt,
                updatedAt
        );
    }

    private CatalogTitle toDomain(CatalogTitleEntity entity) {
        return new CatalogTitle(
                entity.getId(),
                entity.getTitleType(),
                entity.getNameEn(),
                entity.getNameOriginal(),
                entity.getReleaseYear(),
                entity.getDescription(),
                entity.getGenres(),
                entity.getCountries()
        );
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private static String escapeLike(String needle) {
        return needle
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
