# Catalog Changelog

Changelog for the `catalog` module.

## [0.1.0] - 2026-08-25

### Added

- `CatalogTitle` and `TitleType` (`FILM`, `TV_SERIES`, `MINI_SERIES`, `TV_SHOW`)
- Name, year, description, genres, and countries validation and canonicalization
- Hidden uniqueness key `nameEnKey` with identity `(nameEnKey, year, type)`
- Ports: `CatalogTitleRepository`, `CatalogIntegrationEventPublisher`
- Versioned events: `CatalogTitleCreatedV1`, `CatalogTitleUpdatedV1`, `CatalogTitleDeletedV1`
- `CatalogService` create/get/list/search/update/delete
- In-memory `CatalogTitleRepository` for tests and early wiring
- Module README
