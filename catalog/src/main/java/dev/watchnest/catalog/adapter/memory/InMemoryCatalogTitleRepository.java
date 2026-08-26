package dev.watchnest.catalog.adapter.memory;

import dev.watchnest.catalog.domain.CatalogTitle;
import dev.watchnest.catalog.domain.CatalogTitleNotFoundException;
import dev.watchnest.catalog.domain.DuplicateCatalogTitleException;
import dev.watchnest.catalog.domain.TitleType;
import dev.watchnest.catalog.port.CatalogTitleRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCatalogTitleRepository implements CatalogTitleRepository {

    private static final Comparator<CatalogTitle> ORDER = Comparator
            .comparing(CatalogTitle::nameEnKey)
            .thenComparingInt(CatalogTitle::year)
            .thenComparing(CatalogTitle::type)
            .thenComparing(CatalogTitle::id);

    private final ConcurrentHashMap<UUID, CatalogTitle> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NaturalKey, CatalogTitle> byNaturalKey = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Override
    public void insert(CatalogTitle title) {
        Objects.requireNonNull(title, "title");
        synchronized (lock) {
            NaturalKey key = NaturalKey.of(title);
            CatalogTitle occupant = byNaturalKey.get(key);
            if (occupant != null) {
                throw new DuplicateCatalogTitleException(occupant);
            }
            if (byId.containsKey(title.id())) {
                throw new IllegalStateException("catalog title id already exists: " + title.id());
            }
            byNaturalKey.put(key, title);
            byId.put(title.id(), title);
        }
    }

    @Override
    public void update(CatalogTitle title) {
        Objects.requireNonNull(title, "title");
        synchronized (lock) {
            CatalogTitle current = byId.get(title.id());
            if (current == null) {
                throw new CatalogTitleNotFoundException(title.id());
            }
            NaturalKey oldKey = NaturalKey.of(current);
            NaturalKey newKey = NaturalKey.of(title);
            if (!oldKey.equals(newKey)) {
                CatalogTitle occupant = byNaturalKey.get(newKey);
                if (occupant != null) {
                    throw new DuplicateCatalogTitleException(occupant);
                }
                byNaturalKey.remove(oldKey);
                byNaturalKey.put(newKey, title);
            } else {
                byNaturalKey.put(newKey, title);
            }
            byId.put(title.id(), title);
        }
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id");
        synchronized (lock) {
            CatalogTitle current = byId.remove(id);
            if (current == null) {
                throw new CatalogTitleNotFoundException(id);
            }
            byNaturalKey.remove(NaturalKey.of(current));
        }
    }

    @Override
    public Optional<CatalogTitle> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        synchronized (lock) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    @Override
    public Optional<CatalogTitle> findByNaturalKey(String nameEnKey, int year, TitleType type) {
        Objects.requireNonNull(nameEnKey, "nameEnKey");
        Objects.requireNonNull(type, "type");
        synchronized (lock) {
            return Optional.ofNullable(byNaturalKey.get(new NaturalKey(nameEnKey, year, type)));
        }
    }

    @Override
    public List<CatalogTitle> findAllSorted() {
        synchronized (lock) {
            return byId.values().stream().sorted(ORDER).toList();
        }
    }

    @Override
    public List<CatalogTitle> findByNameEnContainingLiteral(String query) {
        String needle = normalizeQuery(query);
        if (needle.isEmpty()) {
            return findAllSorted();
        }
        synchronized (lock) {
            return byId.values().stream()
                    .filter(title -> title.nameEn().toLowerCase(Locale.ROOT).contains(needle))
                    .sorted(ORDER)
                    .toList();
        }
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private record NaturalKey(String nameEnKey, int year, TitleType type) {

        static NaturalKey of(CatalogTitle title) {
            return new NaturalKey(title.nameEnKey(), title.year(), title.type());
        }
    }
}
