# Planner App Changelog

Changelog for the `planner-app` module.

## [0.10.1] - 2026-08-31

### Fixed

- HTTP Testcontainers PostgreSQL is started once in a static initializer, not
  via `@Testcontainers` / `@Container`, so later suites keep the same mapped
  port when they reuse a Spring context
- Auth MockMvc CSRF round-trip uses the named `XSRF-TOKEN` cookie (or Set-Cookie),
  not `getCookies()`
- Viewer HTTP tests use cookie CSRF (`AuthTestSupport.spaCsrf`) instead of
  Spring Security Test `csrf()`, which replaces `CookieCsrfTokenRepository` on
  the shared `CsrfFilter` with session CSRF (`X-CSRF-TOKEN`)

## [0.10.0] - 2026-08-31

### Removed

- Memory profile and in-memory runtime beans (`application-memory.properties`,
  memory identity/library/CMS wiring, logging event publishers)

### Changed

- Default profile is `persistent`; PostgreSQL is required for `bootRun`
- Declarative `@Transactional` on `PersonalLibraryService`,
  `RegistrationService.register`, and `CatalogFacade` create/update/delete
  (`readOnly` on get/search)
- HTTP tests share one Testcontainers PostgreSQL 18 container
- `:planner-app:test` is the only test task (no `persistentHttpTest`)

## [0.9.2] - 2026-08-30

### Fixed

- Ignore blank leftover `WATCHNEST_CMS_XSRF_TOKEN` (and blank session cookies)
  when several cookies share the name; CSRF succeeds if the header matches any
  non-blank cookie of that name

## [0.9.1] - 2026-08-29

### Fixed

- `GET` CSRF responses set `Cache-Control: no-store` (viewer `/api/v1/auth/csrf`
  and CMS `/cms/api/v1/csrf`) so logout cannot leave a cached token after the
  CSRF cookie is cleared

## [0.9.0] - 2026-08-28

### Added

- Liquibase 008 `cms_account.demo` (`BOOLEAN NOT NULL DEFAULT FALSE`)
- Login snapshots `demo` onto the CMS session; idle touch keeps the snapshot
- `403` `demo_account` on demo POST/PUT/DELETE `/cms/api/v1/titles` before
  `CatalogFacade`; catalog rows and catalog events are unchanged

### Changed

- Login and `GET /me` remain `{id, username}` and do not expose `demo`

## [0.8.1] - 2026-08-26

### Fixed

- Persistent CMS HTTP tests bind `cms_account.created_at` as `Timestamp`, not
  `Instant`, so pgJDBC accepts TIMESTAMPTZ

## [0.8.0] - 2026-08-26

### Added

- Isolated CMS API at `/cms/api/v1` (csrf, login, logout, me, titles)
- Lookup-only `cms_account` (no registration or account-management API)
- Opaque `WATCHNEST_CMS_SESSION` cookie (not a second `HttpSession`);
  in-memory, 30-minute idle timeout, reset on process restart
- Catalog CRUD/search on `catalog_title`
- Liquibase 006 `cms_account` and 007 `catalog_title`

### Changed

- Viewer `JSESSIONID` and `/api/v1` stay unchanged; CMS uses its own
  stateless filter chain and CSRF cookies

## [0.7.1] - 2026-08-22

### Fixed

- Over-quota PlanToday API test sets weekday and weekend limits to 2 so it
  does not depend on the default weekend quota

## [0.7.0] - 2026-08-19

### Added

- `PUT /api/v1/library-preferences` and dashboard `treatPlanAsWatched`
- Liquibase 005 `treat_plan_as_watched` (default false)
- Flag on: roll archives remaining PlanToday lines and missed forward
  (`RECORDED_AS_WATCHED`)
- `LibraryPreferencesUpdated`; removal reason `RECORDED_AS_WATCHED`

### Changed

- Title caps use `LibraryLimits.MAX_TITLES_PER_DATE`
- Archive POST projects ensure-induced writes on the requested date

## [0.6.0] - 2026-08-18

### Added

- Past-only `POST /api/v1/watch-events` (`watchedOn` before server today)
- `PATCH` title and `DELETE` for past archive events
- Integration events `WatchEventCorrected` and `WatchEventDeleted`

## [0.5.0] - 2026-08-17

### Added

- PlanToday HTTP: add, check, and remove lines for the working day
- Dated forward plan GET/POST/DELETE (`plannedFor` after today)
- Liquibase 004: `forward_plan_item`, `plan_today`, `plan_today_line`
- Events `PlanTodayRolled`, `ForwardPlanItemAdded`, `ForwardPlanItemRemoved`

### Changed

- Dashboard returns `planToday` and quota from plan-line counts
- `ensurePlanToday` on library requests: expire missed dates, MOVE today, flush checked lines on roll

### Removed

- `POST /api/v1/watch-events`

## [0.4.1] - 2026-08-16

### Added

- `persistentHttpTest`: focused HTTP suite on ephemeral PostgreSQL 18 via Testcontainers
- Testcontainers dependencies managed by the Spring Boot BOM

### Changed

- Default `:planner-app:test` stays memory-only; PostgreSQL HTTP coverage is `:planner-app:persistentHttpTest`

## [0.4.0] - 2026-08-14

### Added

- GET /api/v1/watch-events?from&to — inclusive archive, max 366 days
- Liquibase 003 index watch_event (owner_id, watched_on)

### Changed

- Dashboard loads only today's watch events, not full owner history

## [0.3.0] - 2026-08-09

### Added

- Liquibase schema for `user_account`, `library_profile`, and `watch_event`
- JPA adapters for `UserAccountRepository` and `PersonalLibraryStore` (`persistent`)
- `memory` / `persistent` profile split (tests stay on `memory`)
- `RegistrationService` creates library profile at register time
- After-commit identity/planner event publishers on `persistent`

### Changed

- In-memory account/library wiring is `@Profile("memory")` only

## [0.2.0] - 2026-08-02

### Added

- Session auth API under `/api/v1/auth` (csrf, register, login, logout, me)
- Spring Security with HTTP session cookie and CSRF for the SPA
- Wiring for `:identity` (BCrypt hasher, in-memory accounts, logging events)
- Per-user in-memory personal library store keyed by authenticated user id
- Public readiness endpoint `GET /actuator/health`

### Changed

- Planner endpoints require an authenticated session; owner id is never taken from clients
- CORS allows credentials for the configured frontend origin

## [0.1.0] - 2026-07-31

### Added

- Spring Boot service with REST API under `/api/v1`
- OpenAPI and Swagger UI via springdoc-openapi
- Personal library dashboard, watch log, and screen-time policy endpoints
- Integration event port for future Kafka publishing
- CORS configuration for the `frontend` dev server
- In-memory personal library state for local development without a database
- Module README
- API and `PersonalLibraryService` tests for happy paths and validation
