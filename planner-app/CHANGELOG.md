# Planner App Changelog

Changelog for the `planner-app` module.

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
