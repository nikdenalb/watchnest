# planner-app

Spring Boot service: HTTP API for the personal watch library and browser auth.

**Version:** `plannerAppVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- REST API under `/api/v1`;
- username/password registration and login with server-side HTTP sessions;
- CSRF protection for browser clients;
- per-user library state (`memory` or durable `persistent`);
- public readiness via Actuator health;
- integration event ports;
- CORS with credentials for a configured web origin;
- PostgreSQL + Liquibase + JPA under the `persistent` profile.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Auth HTTP | `AuthApiController`: CSRF, register, login, logout, me |
| Planner HTTP | `PlannerApiController`: dashboard, watch log, policy update |
| Identity wiring | BCrypt hasher; profile-split account repository; identity events |
| Library | `PersonalLibraryService` + `PersonalLibraryStore` keyed by user UUID |
| Security | Spring Security session, CSRF, JSON 401/403 |
| Events | Sync publishers on `memory`; after-commit publishers on `persistent` |
| Persistence | Liquibase-owned schema; JPA entities/adapters only in `planner-app` |

## Profiles

| Profile | Default | Behavior |
| --- | --- | --- |
| `memory` | yes (`spring.profiles.default`) | In-memory accounts + library; no DataSource |
| `persistent` | used by root `./gradlew dev` | Shared local PostgreSQL; accounts + library durable |

`./gradlew :planner-app:test` stays on `memory` — no database required.

Root `./gradlew dev` / `scripts/dev.*` use **`persistent`** with ignored
`.env.planner-app` and one shared local database `watchnest` (no second/test DB).

HTTP session may reset on process restart; durable account/library rows survive.
Docker Compose verification is deferred to a future stage.

## Auth endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/auth/csrf` | public | CSRF header name + token |
| `POST` | `/api/v1/auth/register` | public + CSRF | Create account, library profile, and session (`201`) |
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

- Tests / plain module `bootRun` use `memory` and need no database.
- Full-stack `./gradlew dev` uses `persistent` and requires local PostgreSQL plus
  `.env.planner-app`.
- One shared local DB; do not wipe it from automated tests.
- Schema stability is **not** supported yet: prefer not to drop casually, but on
  migration/checksum errors reset the local DB (or clear Liquibase history) and
  re-apply. Stable migrate/rollback discipline starts only after an explicit
  project decision.
- Default CORS origin: `http://localhost:5173` (`watchnest.frontend.origin`), credentials allowed.
- Passwords are hashed with BCrypt; never returned or logged.
- Hibernate must not create or update schema (`ddl-auto=validate` on `persistent`).
- JVM tests via MockMvc on `memory` (no container or DB required).

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
    persistence/
      jpa/
    security/
  src/main/resources/
    application.properties
    application-memory.properties
    application-persistent.properties
    db/changelog/
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

Default port: `8080`. Default profile for module tasks: `memory`.

Full-stack local run (API + UI, **persistent** / PostgreSQL):

```powershell
# once: copy example → ignored .env.planner-app and set real passwords
.\gradlew.bat dev
```

Config template: `config/examples/planner-app.env.example` (placeholders only).
`scripts/dev.*` load `.env.planner-app` automatically; plain Gradle `bootRun`
does not.

## Seed data

On register / first library access for a user:

- `displayName`: canonical username
- weekday limit: `2`
- weekend limit: `4`
- watch log: empty

## Scope

In scope: auth session/CSRF, durable accounts + library on `persistent`,
per-user isolation, dashboard, watch log, policy update, event publishers,
CORS with credentials, health readiness, Liquibase schema for
`user_account` / `library_profile` / `watch_event`.

Out of scope: Docker Compose, Kafka producer adapter, OAuth/email,
multi-profile household model, product `0.3.0`.
