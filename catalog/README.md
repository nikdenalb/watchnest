# catalog

Java library: owned title catalog, uniqueness rules, and catalog integration events.

**Version:** `catalogVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- store works (`CatalogTitle`) with type, English name, original name, and year;
- canonicalize optional description, genres, and countries;
- publish versioned create/update/delete facts after successful mutations.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Title | `CatalogTitle`: id, `TitleType`, `nameEn`, `nameOriginal`, `year`, optional `description` / `genres` / `countries` |
| Type | `TitleType`: `FILM`, `TV_SERIES`, `MINI_SERIES`, `TV_SHOW` |
| Names | trim; required; 1–255 characters; preserve case and internal whitespace |
| Key | hidden `nameEnKey`: trimmed `nameEn` lower-cased with `Locale.ROOT` |
| Year | required integer `1000`–`9999` |
| Description | trim; blank → `null`; at most 10,000 characters |
| Tags | `genres` / `countries`: split on comma, trim, `Locale.ROOT` lower-case, drop empty tokens, keep order and duplicates, join with `", "`; blank/empty → `null`; canonical length at most 1,000 |
| Uniqueness | unique `(nameEnKey, year, type)`; case-only English-name changes are the same work |
| Storage | `CatalogTitleRepository` insert/update enforce uniqueness; delete is hard delete |
| Events | `CatalogTitleCreatedV1` / `UpdatedV1` / `DeletedV1` after successful mutations |
| Use cases | `CatalogService`: create, get, list, search, update, delete |

## Public surface

```text
CatalogService
  create(type, nameEn, nameOriginal, year, description, genres, countries) -> CatalogTitle
  get(id) -> CatalogTitle
  list() -> List<CatalogTitle>
  search(query) -> List<CatalogTitle>
  update(id, type, nameEn, nameOriginal, year, description, genres, countries) -> CatalogTitle
  delete(id)

CatalogTitle {
  UUID id,
  TitleType type,
  String nameEn,
  String nameOriginal,
  int year,
  String description?,
  String genres?,
  String countries?
}
  nameEnKey() -> String

Ports:
  CatalogTitleRepository
  CatalogIntegrationEventPublisher

Events (eventId, occurredAt, normalized CatalogTitle snapshot):
  CatalogTitleCreatedV1
  CatalogTitleUpdatedV1
  CatalogTitleDeletedV1
```

Update replaces every field and keeps `id`. Missing get/update/delete is not found.
Search trims `query`, lower-cases with `Locale.ROOT`, and matches a literal
case-insensitive substring of `nameEn`. Blank query lists all. `%` and `_` are
ordinary characters. Results sort by `nameEnKey`, year, type, then id.

## Error categories

| Exception | Meaning |
| --- | --- |
| `InvalidCatalogTitleException` | name, year, description, genres, or countries invalid |
| `DuplicateCatalogTitleException` | `(nameEnKey, year, type)` already exists; carries `existingTitle` |
| `CatalogTitleNotFoundException` | unknown title id |

## Constraints

- JDK only (no Spring, JPA, HTTP, or session types in this module).
- Domain and ports without I/O frameworks; persistence is a port.
- JVM unit tests (no container or network).

## Layout

```text
catalog/
  src/main/java/dev/watchnest/catalog/
    domain/
    port/
    service/
    adapter/memory/
  src/test/java/...
  build.gradle.kts
  gradle.properties
  CHANGELOG.md
  README.md
```

## Build and test

Requires root `settings.gradle.kts` to `include("catalog")`.

```bash
./gradlew :catalog:test
./gradlew :catalog:build
```

Windows:

```powershell
.\gradlew.bat :catalog:test
```

## Scope (0.1.0)

In scope: title types, normalization, uniqueness key, CRUD/list/search, hard
delete, in-memory repository, create/update/delete integration events.

Out of scope: episodes, seasons, people/credits, posters, runtime, ratings,
reviews, external catalog ids, genre/country dictionaries.
