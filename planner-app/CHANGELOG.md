# Planner App Changelog

Changelog for the `planner-app` module.

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
