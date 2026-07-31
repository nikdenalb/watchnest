# planner-app

Spring Boot service: HTTP API for the personal watch library.

**Version:** `plannerAppVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- REST API under `/api/v1`;
- OpenAPI / Swagger UI;
- in-memory library state for local runs;
- integration event port for future messaging adapters;
- CORS for a configured web origin.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| HTTP | `PlannerApiController`: dashboard, watch log, policy update |
| DTOs | request/response records under `api.dto` |
| Library | `PersonalLibraryService`: in-memory profile, events, quota queries |
| Config | OpenAPI, CORS, `Clock`, quota calculator bean |
| Events | `IntegrationEventPublisher` + logging publisher implementation |

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/dashboard` | Today’s quota, policy, watch log |
| `POST` | `/api/v1/watch-events` | Log a watch for today |
| `PUT` | `/api/v1/policy` | Update weekday / weekend limits |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Constraints

- No database; process restart clears state.
- Default CORS origin: `http://localhost:5173` (`watchnest.frontend.origin`).
- JVM tests via MockMvc (no container required).

## Layout

```text
planner-app/
  src/main/java/dev/watchnest/plannerapp/
    api/
    config/
    library/
    integration/
  src/main/resources/application.properties
  src/test/java/...
  build.gradle.kts
  gradle.properties
  CHANGELOG.md
  README.md
```

## Build and run

```bash
./gradlew :planner-app:test
./gradlew :planner-app:bootRun
```

Windows:

```powershell
.\gradlew.bat :planner-app:bootRun
```

Default port: `8080`.

Config template: `config/examples/planner-app.env.example`.

## Seed data (in-memory)

On startup:

- `displayName`: `You`
- weekday limit: `2`
- weekend limit: `4`
- watch log: empty

## Scope (0.1.0)

In scope: dashboard, watch log, policy update, logging event publisher, CORS,
in-memory seed profile.

Out of scope: database persistence, Kafka producer adapter, authentication,
multi-profile household model.
