# Frontend Changelog

Changelog for the `frontend` module.

## [0.4.0] - 2026-08-17

### Added

- PlanToday list: checkbox, add, and remove for the working day
- Dated forward plan with Week / Month / Year display ranges (ISO week Mon–Sun; calendar month/year)

### Changed

- Quota card shows Planned from plan-line counts; add stays enabled at remaining 0
- Day-change refresh invalidates dashboard and forward-plan queries

### Removed

- Log-today form and `logWatchEvent` client (`POST /watch-events`)

## [0.3.1] - 2026-08-15

### Changed

- Require Node 24 LTS (`engines.node` `>=24 <25`, `.nvmrc`)

## [0.3.0] - 2026-08-14

### Added

- Watch history month list on the dashboard (from dashboard.today, not the browser clock)

## [0.2.0] - 2026-08-02

### Added

- Register / login screens and session-gated dashboard
- Auth API client with `credentials: "include"` and CSRF header handling
- Logout and 401 handling that clears user-scoped client state

### Changed

- Splash readiness waits on `/api/v1/auth/me`, not the protected dashboard
- Planner mutations send CSRF; one retry on stale CSRF token
- Dashboard shell extracted; auth success no longer depends on post-login CSRF refresh
## [0.1.0] - 2026-07-31

### Added

- React + Vite + TypeScript SPA for the personal watch library (dark theme only)
- Dashboard consuming `/api/v1` from `planner-app`
- Watch log and screen-time policy forms backed by the REST API
- Module README
- Thin Gradle orchestration (`build.gradle.kts`) so root can build/run via `./gradlew`
- Vitest tests for splash, dashboard flows, API client, and day-change refresh
