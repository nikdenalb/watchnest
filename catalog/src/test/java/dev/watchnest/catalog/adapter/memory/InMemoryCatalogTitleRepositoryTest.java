package dev.watchnest.catalog.adapter.memory;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.CatalogTitleNotFoundException;
import dev.watchnest.catalog.domain.DuplicateCatalogTitleException;
import dev.watchnest.catalog.domain.TitleType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCatalogTitleRepositoryTest {

    @Test
    void insertRejectsDuplicateNaturalKeyAtomicallyUnderContention() throws Exception {
        InMemoryCatalogTitleRepository repository = new InMemoryCatalogTitleRepository();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();

        try {
            @SuppressWarnings("unchecked")
            Future<?>[] futures = new Future[threads];
            for (int i = 0; i < threads; i++) {
                UUID id = UUID.fromString(String.format("10000000-0000-0000-0000-%012d", i + 1));
                CatalogTitle title = new CatalogTitle(
                        id,
                        TitleType.FILM,
                        "Dune",
                        "Dune",
                        2021,
                        null,
                        null,
                        null
                );
                futures[i] = pool.submit(() -> {
                    start.await();
                    try {
                        repository.insert(title);
                        successes.incrementAndGet();
                    } catch (DuplicateCatalogTitleException e) {
                        duplicates.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(threads - 1, duplicates.get());
        assertTrue(repository.findByNaturalKey("dune", 2021, TitleType.FILM).isPresent());
        assertEquals(1, repository.findAllSorted().size());
    }

    @Test
    void sequentialDuplicateThrowsWithExistingTitle() {
        InMemoryCatalogTitleRepository repository = new InMemoryCatalogTitleRepository();
        CatalogTitle first = title("20000000-0000-0000-0000-000000000001", TitleType.FILM, "Dune", 2021);
        repository.insert(first);

        DuplicateCatalogTitleException duplicate = assertThrows(
                DuplicateCatalogTitleException.class,
                () -> repository.insert(title("20000000-0000-0000-0000-000000000002", TitleType.FILM, "DUNE", 2021))
        );

        assertEquals(first, duplicate.existingTitle());
    }

    @Test
    void listAndSearchSortAndTreatWildcardsLiterally() {
        InMemoryCatalogTitleRepository repository = new InMemoryCatalogTitleRepository();
        CatalogTitle alphaFilm = title("00000000-0000-0000-0000-000000000001", TitleType.FILM, "Alpha", 2020);
        CatalogTitle alphaSeries = title("00000000-0000-0000-0000-000000000002", TitleType.TV_SERIES, "ALPHA", 2020);
        CatalogTitle percent = title("00000000-0000-0000-0000-000000000003", TitleType.FILM, "100% Wolf", 2020);
        CatalogTitle underscore = title("00000000-0000-0000-0000-000000000004", TitleType.FILM, "Under_score", 2020);
        repository.insert(underscore);
        repository.insert(percent);
        repository.insert(alphaSeries);
        repository.insert(alphaFilm);

        assertEquals(List.of(percent, alphaFilm, alphaSeries, underscore), repository.findAllSorted());
        assertEquals(List.of(percent, alphaFilm, alphaSeries, underscore), repository.findByNameEnContainingLiteral(" "));
        assertEquals(List.of(alphaFilm, alphaSeries), repository.findByNameEnContainingLiteral("alp"));
        assertEquals(List.of(percent), repository.findByNameEnContainingLiteral("%"));
        assertEquals(List.of(underscore), repository.findByNameEnContainingLiteral("_"));
        assertTrue(repository.findByNameEnContainingLiteral("100_").isEmpty());
    }

    @Test
    void updateAndDeleteMissingThrowNotFound() {
        InMemoryCatalogTitleRepository repository = new InMemoryCatalogTitleRepository();
        UUID missing = UUID.fromString("30000000-0000-0000-0000-000000000099");
        CatalogTitle unknown = new CatalogTitle(
                missing,
                TitleType.FILM,
                "Gone",
                "Gone",
                2021,
                null,
                null,
                null
        );

        assertThrows(CatalogTitleNotFoundException.class, () -> repository.update(unknown));
        assertThrows(CatalogTitleNotFoundException.class, () -> repository.delete(missing));
        assertTrue(repository.findById(missing).isEmpty());
    }

    @Test
    void hardDeleteReleasesNaturalKey() {
        InMemoryCatalogTitleRepository repository = new InMemoryCatalogTitleRepository();
        CatalogTitle first = title("40000000-0000-0000-0000-000000000001", TitleType.FILM, "Dune", 2021);
        repository.insert(first);
        repository.delete(first.id());

        CatalogTitle second = title("40000000-0000-0000-0000-000000000002", TitleType.FILM, "Dune", 2021);
        repository.insert(second);

        assertEquals(second, repository.findById(second.id()).orElseThrow());
        assertTrue(repository.findById(first.id()).isEmpty());
    }

    private static CatalogTitle title(String id, TitleType type, String nameEn, int year) {
        return new CatalogTitle(UUID.fromString(id), type, nameEn, nameEn, year, null, null, null);
    }
}
