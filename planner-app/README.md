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
| Planner HTTP | `PlannerApiController`: dashboard, PlanToday, dated forward plan, archive, policy |
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

`:planner-app:test` stays on `memory` — no database and no Docker.

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
| `GET` | `/api/v1/dashboard` | Today’s quota, policy, and PlanToday |
| `POST` | `/api/v1/plan/today/lines` | Add a PlanToday line (`source=MANUAL`, `201`) |
| `PATCH` | `/api/v1/plan/today/lines/{id}` | Set `checked` on a PlanToday line |
| `DELETE` | `/api/v1/plan/today/lines/{id}` | Remove a PlanToday line (`204`) |
| `GET` | `/api/v1/plan/forward?from=&to=` | Dated forward items in an inclusive range |
| `POST` | `/api/v1/plan/forward` | Add a forward item with `plannedFor` after today (`201`) |
| `DELETE` | `/api/v1/plan/forward/{id}` | Remove a forward item (`204`) |
| `GET` | `/api/v1/watch-events` | Archive: events in `from`–`to` (ISO-8601 dates, both required, inclusive) |
| `PUT` | `/api/v1/policy` | Update weekday / weekend limits |

Unsafe plan mutations need CSRF. `POST /api/v1/watch-events` is removed.

PlanToday is the editable working day (`forDate` = server today). Quota counts
PlanToday **lines** (`episodesPlanned` / `canAddAnotherEpisode`), not archive
rows and not checkmarks. Adding a line is allowed when over quota (`201`).
Checked lines become `WatchEvent` rows only when the day rolls; same-day titles
are not in the archive.

The first valid authenticated **library** request (dashboard, archive GET,
forward GET, policy, PlanToday/forward mutations — not auth) runs
`ensurePlanToday`: missed forward items (`plannedFor < today`) are deleted;
items dated today MOVE into PlanToday (`source=FORWARD`) and leave the forward
plan; a stale PlanToday (`forDate < today`) flushes checked lines to the
archive (`watchedOn` = the closed date) and discards unchecked lines. Auth
`/me`, CSRF, login, register, and logout do not roll.

Forward plan is one dated collection. Week / month / year are display ranges
over `GET /plan/forward` (ISO week Monday–Sunday; calendar month/year), not
stored horizons. `POST /plan/forward` rejects today and past dates; add a
title for today through PlanToday.

Archive and forward `from`/`to` must satisfy `from <= to` and an inclusive span
of at most 366 days. Empty ranges return `200` with an empty list. Caps: 50
PlanToday lines and 50 forward items per `plannedFor` date (`400` when an add
exceeds). Unknown or other-owner ids are `404`. Stored PlanToday with
`forDate > today` is `409` `plan_date_conflict`. Owner UUID comes from the
authenticated principal only.

Swagger UI: `http://localhost:8080/swagger-ui.html`

Readiness: `GET /actuator/health` (public). Other actuator endpoints are not exposed.

## Constraints

- Tests / plain module `bootRun` use `memory` and need no database.
- Full-stack `./gradlew dev` uses `persistent` and requires local PostgreSQL plus
  `.env.planner-app`.
- One shared local DB; do not wipe it from automated tests.
- Automated tests never use `localhost:5432/watchnest`.
- Schema stability is **not** supported yet: prefer not to drop casually, but on
  migration/checksum errors reset the local DB (or clear Liquibase history) and
  re-apply. Stable migrate/rollback discipline starts only after an explicit
  project decision.
- Default CORS origin: `http://localhost:5173` (`watchnest.frontend.origin`), credentials allowed.
- Passwords are hashed with BCrypt; never returned or logged.
- Hibernate must not create or update schema (`ddl-auto=validate` on `persistent`).
- `:planner-app:test` = unit + memory HTTP MockMvc; no Docker.
- `:planner-app:persistentHttpTest` = HTTP against ephemeral PostgreSQL 18
  (requires Docker). It never uses the shared local `watchnest` DB.
- `./gradlew build` does not run `persistentHttpTest`.

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
./gradlew :planner-app:persistentHttpTest
./gradlew :planner-app:bootRun
```

`:planner-app:test` is unit + memory HTTP and needs no Docker.
`:planner-app:persistentHttpTest` is the PostgreSQL HTTP suite; run it when
Docker is available. `./gradlew build` does not include that task.

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
- PlanToday: created on first library request for today; empty until MOVE or add
- forward plan: empty
- watch archive: empty until a day roll flushes checked lines

## Scope

In scope: auth session/CSRF, durable accounts + library on `persistent`,
per-user isolation, dashboard PlanToday, dated forward plan GET/POST/DELETE,
date-range archive GET, policy update, event publishers, CORS with credentials,
health readiness, Liquibase schema for `user_account` / `library_profile` /
`watch_event` plus `003` owner/date index and `004` PlanToday / forward plan.

Out of scope: calendar grid, logging a watch on a past date, catch-up for
skipped days, leftover-title return, edit/delete archive rows, Docker Compose,
Kafka producer adapter, OAuth/email, multi-profile household model.
