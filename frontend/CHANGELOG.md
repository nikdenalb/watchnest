# Frontend Changelog

Changelog for the `frontend` module.

## [0.1.0] - 2026-07-31

### Added

- React + Vite + TypeScript SPA for the personal watch library (dark theme only)
- Dashboard consuming `/api/v1` from `planner-app`
- Watch log and screen-time policy forms backed by the REST API
- Module README
- Thin Gradle orchestration (`build.gradle.kts`) so root can build/run via `./gradlew`
- Vitest tests for splash, dashboard flows, API client, and day-change refresh
