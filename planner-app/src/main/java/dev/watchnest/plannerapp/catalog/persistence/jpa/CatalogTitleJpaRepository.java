package dev.watchnest.plannerapp.catalog.persistence.jpa;

import dev.watchnest.catalog.domain.TitleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogTitleJpaRepository extends JpaRepository<CatalogTitleEntity, UUID> {

    Optional<CatalogTitleEntity> findByNameEnKeyAndReleaseYearAndTitleType(
            String nameEnKey,
            int releaseYear,
            TitleType titleType
    );

    @Query("""
            select t from CatalogTitleEntity t
            where lower(t.nameEn) like :pattern escape '!'
            """)
    List<CatalogTitleEntity> searchLiteral(@Param("pattern") String pattern);
}
