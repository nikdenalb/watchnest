package dev.watchnest.plannerapp.catalog;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.DuplicateCatalogTitleException;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.service.CatalogService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class CatalogFacade {

    private static final String NATURAL_KEY_UK = "uk_catalog_title_natural_key";

    private final CatalogService catalogService;
    private final CatalogNaturalKeyLookup naturalKeys;

    public CatalogFacade(CatalogService catalogService, CatalogNaturalKeyLookup naturalKeys) {
        this.catalogService = catalogService;
        this.naturalKeys = naturalKeys;
    }

    @Transactional
    public CatalogTitle create(
            TitleType type,
            String nameEn,
            String nameOriginal,
            int year,
            String description,
            String genres,
            String countries
    ) {
        return executeMutation(
                () -> catalogService.create(type, nameEn, nameOriginal, year, description, genres, countries),
                type,
                nameEn,
                year
        );
    }

    @Transactional(readOnly = true)
    public CatalogTitle get(UUID id) {
        return catalogService.get(id);
    }

    @Transactional(readOnly = true)
    public List<CatalogTitle> search(String query) {
        return catalogService.search(query);
    }

    @Transactional
    public CatalogTitle update(
            UUID id,
            TitleType type,
            String nameEn,
            String nameOriginal,
            int year,
            String description,
            String genres,
            String countries
    ) {
        return executeMutation(
                () -> catalogService.update(id, type, nameEn, nameOriginal, year, description, genres, countries),
                type,
                nameEn,
                year
        );
    }

    @Transactional
    public void delete(UUID id) {
        catalogService.delete(id);
    }

    private CatalogTitle executeMutation(
            Supplier<CatalogTitle> action,
            TitleType type,
            String nameEn,
            int year
    ) {
        try {
            return action.get();
        } catch (DuplicateCatalogTitleException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (!isUniqueViolation(ex)) {
                throw ex;
            }
            CatalogTitle canonical = new CatalogTitle(
                    UUID.randomUUID(),
                    type,
                    nameEn,
                    nameEn,
                    year,
                    null,
                    null,
                    null
            );
            CatalogTitle existing = naturalKeys.find(canonical.nameEnKey(), canonical.year(), canonical.type())
                    .orElseThrow(() -> ex);
            throw new DuplicateCatalogTitleException(existing);
        }
    }

    private static boolean isUniqueViolation(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof DataIntegrityViolationException && messageMentionsNaturalKey(cursor)) {
                return true;
            }
            if (messageMentionsNaturalKey(cursor) || sqlStateUnique(cursor)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static boolean messageMentionsNaturalKey(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(NATURAL_KEY_UK);
    }

    private static boolean sqlStateUnique(Throwable throwable) {
        return throwable instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }
}
