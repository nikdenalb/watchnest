package dev.watchnest.plannerapp.catalog;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.port.CatalogTitleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
class CatalogNaturalKeyLookup {

    private final CatalogTitleRepository titles;

    CatalogNaturalKeyLookup(CatalogTitleRepository titles) {
        this.titles = titles;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    Optional<CatalogTitle> find(String nameEnKey, int year, TitleType type) {
        return titles.findByNaturalKey(nameEnKey, year, type);
    }
}
