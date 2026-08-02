# planner-app

Spring Boot service: HTTP API for the personal watch library and browser auth.

**Version:** `plannerAppVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- REST API under `/api/v1`;
- username/password registration and login with server-side HTTP sessions;
- CSRF protection for browser clients;
- per-user in-memory library state for local runs;
- public readiness via Actuator health;
- integration event ports;
- CORS with credentials for a configured web origin.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Auth HTTP | `AuthApiController`: CSRF, register, login, logout, me |
| Planner HTTP | `PlannerApiController`: dashboard, watch log, policy update |
| Identity wiring | BCrypt hasher, logging identity events, in-memory accounts |
| Library | `PersonalLibraryService` + `PersonalLibraryStore` keyed by user UUID |
| Security | Spring Security session, CSRF, JSON 401/403 |
| Events | Planner `IntegrationEventPublisher` + logging publisher |

## Auth endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/auth/csrf` | public | CSRF header name + token |
| `POST` | `/api/v1/auth/register` | public + CSRF | Create account and session (`201`) |
| `POST` | `/api/v1/auth/login` | public + CSRF | Authenticate and create session |
| `POST` | `/api/v1/auth/logout` | public + CSRF | Invalidate session (`204`, idempotent) |
| `GET` | `/api/v1/auth/me` | session | Current user id + username |

Session: `JSESSIONID` (`HttpOnly`, `SameSite=Lax`; `Secure` via `watchnest.session.cookie.secure`).
Unsafe requests need the CSRF header from `GET /api/v1/auth/csrf` (refresh after register/login/logout).

## Planner endpoints (session required)

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/dashboard` | Today’s quota, policy, watch log |
| `POST` | `/api/v1/watch-events` | Log a watch for today |
| `PUT` | `/api/v1/policy` | Update weekday / weekend limits |

Owner UUID comes from the authenticated principal only. Request bodies must not supply `ownerId`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

Readiness: `GET /actuator/health` (public). Other actuator endpoints are not exposed.

## Constraints

- No database; process restart clears accounts, sessions, and library state.
- Default CORS origin: `http://localhost:5173` (`watchnest.frontend.origin`), credentials allowed.
- Passwords are hashed with BCrypt; never returned or logged.
- JVM tests via MockMvc (no container required).

## Layout

```text
planner-app/
  src/main/java/dev/watchnest/plannerapp/
    api/
    auth/
    config/
    identity/
    library/
    integration/
    security/
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

On first authenticated access for a user:

- `displayName`: canonical username
- weekday limit: `2`
- weekend limit: `4`
- watch log: empty

## Scope

In scope: auth session/CSRF, per-user library isolation, dashboard, watch log,
policy update, logging event publishers, CORS with credentials, health readiness.

Out of scope: database persistence, Kafka producer adapter, OAuth/email,
multi-profile household model.
