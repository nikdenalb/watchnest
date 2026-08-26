package dev.watchnest.plannerapp.catalog;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.DuplicateCatalogTitleException;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.port.CatalogTitleRepository;
import dev.watchnest.catalog.service.CatalogService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class CatalogFacade {

    private static final String NATURAL_KEY_UK = "uk_catalog_title_natural_key";

    private final CatalogService catalogService;
    private final CatalogTitleRepository titles;
    private final ObjectProvider<PlatformTransactionManager> transactionManagers;

    public CatalogFacade(
            CatalogService catalogService,
            CatalogTitleRepository titles,
            ObjectProvider<PlatformTransactionManager> transactionManagers
    ) {
        this.catalogService = catalogService;
        this.titles = titles;
        this.transactionManagers = transactionManagers;
    }

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

    public CatalogTitle get(UUID id) {
        return catalogService.get(id);
    }

    public List<CatalogTitle> search(String query) {
        return catalogService.search(query);
    }

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

    public void delete(UUID id) {
        inTransaction(() -> {
            catalogService.delete(id);
            return null;
        });
    }

    private CatalogTitle executeMutation(
            Supplier<CatalogTitle> action,
            TitleType type,
            String nameEn,
            int year
    ) {
        try {
            return inTransaction(action);
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
            CatalogTitle existing = titles.findByNaturalKey(canonical.nameEnKey(), canonical.year(), canonical.type())
                    .orElseThrow(() -> ex);
            throw new DuplicateCatalogTitleException(existing);
        }
    }

    private <T> T inTransaction(Supplier<T> action) {
        PlatformTransactionManager manager = transactionManagers.getIfAvailable();
        if (manager == null) {
            return action.get();
        }
        return new TransactionTemplate(manager).execute(status -> action.get());
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
