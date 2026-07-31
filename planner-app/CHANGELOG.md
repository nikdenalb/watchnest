# Planner App Changelog

Changelog for the `planner-app` module.

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
