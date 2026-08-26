package dev.watchnest.catalog.port;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.TitleType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogTitleRepository {

    void insert(CatalogTitle title);

    void update(CatalogTitle title);

    void delete(UUID id);

    Optional<CatalogTitle> findById(UUID id);

    Optional<CatalogTitle> findByNaturalKey(String nameEnKey, int year, TitleType type);

    List<CatalogTitle> findAllSorted();

    List<CatalogTitle> findByNameEnContainingLiteral(String query);
}
