# Planner App Changelog

Changelog for the `planner-app` module.

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
