# planner-app

Spring Boot service: HTTP API for the personal watch library, browser auth, and the CMS catalog editor API.

**Version:** `plannerAppVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- REST API under `/api/v1` and CMS API under `/cms/api/v1`;
- username/password registration and login with server-side HTTP sessions;
- isolated CMS authentication with an opaque cookie (not a second `HttpSession`);
- CSRF protection for browser clients (viewer and CMS cookies are independent);
- per-user durable library state;
- owned title catalog persistence for the CMS editor;
- public readiness via Actuator health;
- integration event ports (planner and catalog);
- CORS with credentials for a configured web origin;
- PostgreSQL + Liquibase + JPA (default `persistent` profile).

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Auth HTTP | `AuthApiController`: CSRF, register, login, logout, me |
| CMS HTTP | `CmsAuthApiController` / `CmsTitleApiController`: CMS CSRF, login, logout, me, titles |
| Planner HTTP | `PlannerApiController`: dashboard, PlanToday, dated forward plan, archive, policy, library preferences |
| Identity wiring | BCrypt hasher; JPA account repository; identity events |
| CMS accounts | Lookup-only `cms_account` (no registration/CRUD API); provisioned out of band; `demo` defaults false |
| Catalog | `CatalogService` via `CatalogFacade`; JPA `catalog_title` |
| Library | `PersonalLibraryService` + `PersonalLibraryStore` keyed by user UUID |
| Security | Viewer session + CSRF; first CMS stateless filter chain + CMS CSRF |
| Events | After-commit publishers |
| Persistence | Liquibase-owned schema; JPA entities/adapters only in `planner-app` |

## Profiles

| Profile | Default | Behavior |
| --- | --- | --- |
| `persistent` | yes (`spring.profiles.default`) | PostgreSQL + Liquibase + JPA; accounts, library, CMS accounts, and catalog durable |

Plain `bootRun` and root `./gradlew dev` / `scripts/dev.*` require PostgreSQL.
Root `dev` uses ignored `.env.planner-app` and one shared local database
`watchnest` (no second/test DB).

HTTP session may reset on process restart; durable account/library rows survive.
CMS sessions stay process-local. Docker Compose verification is deferred.

`:planner-app:test` HTTP suites use ephemeral PostgreSQL 18 via Testcontainers
(Docker required). Domain unit tests (`PersonalLibraryServiceTest`) stay
database-free.

## Auth endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/auth/csrf` | public | CSRF header name + token |
| `POST` | `/api/v1/auth/register` | public + CSRF | Create account, library profile, and session (`201`) |
| `POST` | `/api/v1/auth/login` | public + CSRF | Authenticate and create session |
| `POST` | `/api/v1/auth/logout` | public + CSRF | Invalidate session (`204`, idempotent) |
| `GET` | `/api/v1/auth/me` | session | Current user id + username |

Session: `JSESSIONID` (`HttpOnly`, `SameSite=Lax`; `Secure` via `watchnest.session.cookie.secure`).
Unsafe requests need the CSRF header from `GET /api/v1/auth/csrf` (refresh after register/login/logout;
that GET sets `Cache-Control: no-store`).

## CMS endpoints

CMS credentials live only in `cms_account`. There is no CMS `/register` and no account-management API.
`cms_account.demo` is `BOOLEAN NOT NULL DEFAULT FALSE` (Liquibase `008`). Existing rows and inserts
that omit the column stay writable editors. Login snapshots `demo` onto the in-memory CMS session
and keeps that snapshot when idle time is touched; an out-of-band flag change applies to new logins
only. Login and `GET /cms/api/v1/me` remain `{ "id", "username" }` and do not expose `demo`.
A demonstration account may authenticate, refresh CSRF, log out, and read titles. POST, PUT, and
DELETE `/cms/api/v1/titles` return `403` `demo_account` with message
`This is a demonstration account. The change was not applied.` after Bean Validation and before
`CatalogFacade`. Catalog rows and catalog events are unchanged. Missing/invalid CMS CSRF remains
`csrf_invalid`. Malformed JSON and missing/null required fields remain `400` `validation_failed`.
Viewer `JSESSIONID` is ignored. CMS uses cookie `WATCHNEST_CMS_SESSION` (256-bit random base64url,
`Path=/cms`, `HttpOnly`, `SameSite=Lax`, `Secure` from `watchnest.session.cookie.secure`). The v1
store is process-local with a 30-minute idle timeout; process restart logs CMS users out.

CMS CSRF is independent of viewer `XSRF-TOKEN`:

| Cookie | Header | Path | HttpOnly |
| --- | --- | --- | --- |
| `WATCHNEST_CMS_XSRF_TOKEN` | `X-WATCHNEST-CMS-XSRF-TOKEN` | `/cms` | false (`SameSite=Lax`, same `Secure` setting) |

`GET /cms/api/v1/csrf` is public, sets `Cache-Control: no-store`, and writes the CSRF cookie. Login, logout, and title writes require it.

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| `GET` | `/cms/api/v1/csrf` | public | CMS CSRF header name + token |
| `POST` | `/cms/api/v1/login` | public + CMS CSRF | Authenticate a provisioned CMS account |
| `POST` | `/cms/api/v1/logout` | public + CMS CSRF | Revoke current CMS token (`204`, idempotent) |
| `GET` | `/cms/api/v1/me` | CMS session | Current CMS user id + username |
| `GET` | `/cms/api/v1/titles?q=` | CMS session | List/search titles (`q` optional; empty = all) |
| `GET` | `/cms/api/v1/titles/{id}` | CMS session | Get one title |
| `POST` | `/cms/api/v1/titles` | CMS session + CMS CSRF | Create title (`201` + `Location`); demo `403` `demo_account` |
| `PUT` | `/cms/api/v1/titles/{id}` | CMS session + CMS CSRF | Full replace; demo `403` `demo_account` |
| `DELETE` | `/cms/api/v1/titles/{id}` | CMS session + CMS CSRF | Hard delete (`204`); demo `403` `demo_account` |

Unknown CMS username, a viewer-only username, and a wrong password all return `401`
`invalid_credentials`. Duplicate English name + year + type returns `409` `title_already_exists`
with `existingTitle`. Missing title `404` `not_found`. CMS login never creates `library_profile`
or `JSESSIONID`.

Catalog integration events (`CatalogTitleCreatedV1` / `UpdatedV1` / `DeletedV1`) publish after
commit. Liquibase `006` `cms_account`, `007` `catalog_title`,
and `008` `cms_account.demo` follow `005`. Rows in `cms_account` are inserted out of band;
production exposes no mutator.

## Planner endpoints (session required)

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/dashboard` | Today’s quota, policy, PlanToday, and `treatPlanAsWatched` |
| `POST` | `/api/v1/plan/today/lines` | Add a PlanToday line (`source=MANUAL`, `201`) |
| `PATCH` | `/api/v1/plan/today/lines/{id}` | Set `checked` on a PlanToday line |
| `DELETE` | `/api/v1/plan/today/lines/{id}` | Remove a PlanToday line (`204`) |
| `GET` | `/api/v1/plan/forward?from=&to=` | Dated forward items in an inclusive range |
| `POST` | `/api/v1/plan/forward` | Add a forward item with `plannedFor` after today (`201`) |
| `DELETE` | `/api/v1/plan/forward/{id}` | Remove a forward item (`204`) |
| `GET` | `/api/v1/watch-events` | Archive: events in `from`–`to` (ISO-8601 dates, both required, inclusive) |
| `POST` | `/api/v1/watch-events` | Add a past archive event (`watchedOn` before server today, `201`) |
| `PATCH` | `/api/v1/watch-events/{id}` | Rename a past archive event (`contentTitle` only) |
| `DELETE` | `/api/v1/watch-events/{id}` | Delete a past archive event (`204`) |
| `PUT` | `/api/v1/policy` | Update weekday / weekend limits |
| `PUT` | `/api/v1/library-preferences` | Set `treatPlanAsWatched` |

Unsafe plan and archive mutations need CSRF. 0.6.0 removed today-log `POST /watch-events`;
this cut adds past-only `POST` (`watchedOn` before today). Today and future dates return
`400` `validation_failed`. Extra `watchedOn` on PATCH is ignored; the stored date never
changes. Missing or other-owner ids return `404` `not_found`.

PlanToday is the editable working day (`forDate` = server today). Quota counts
PlanToday **lines** (`episodesPlanned` / `canAddAnotherEpisode`), not archive
rows and not checkmarks. Adding a line is allowed when over quota (`201`).
With `treatPlanAsWatched` false (default), checked lines become `WatchEvent`
rows when the day rolls; unchecked lines are discarded. With the flag true,
PlanToday is the watch log: add/remove only, PATCH `checked` returns `400`,
new and MOVE lines are stored checked, and **all** remaining lines archive on
roll. Same-day titles are not in the archive until roll.

`PUT /library-preferences` is a library request: persist the flag, then
`ensurePlanToday`. Turning the flag on then checks every current PlanToday
line. Turning it off does not uncheck. Missing/null body → `400` before
ensure. Future-plan conflict leaves the stored flag unchanged.
`LibraryPreferencesUpdated` is published only when the value changes.

The first valid authenticated **library** request (dashboard, archive GET,
valid archive POST/PATCH/DELETE, forward GET, policy, library preferences,
PlanToday/forward mutations — not auth) runs `ensurePlanToday`. Flag false:
missed forward (`plannedFor < today`) is deleted; items dated today MOVE into
PlanToday (`source=FORWARD`, unchecked) and leave the forward plan; a stale
PlanToday flushes checked lines to the archive (`watchedOn` = the closed date)
and discards unchecked lines. Flag true: missed forward is archived
(`watchedOn` = `plannedFor`, removal `RECORDED_AS_WATCHED`) instead of
expired; stale PlanToday flushes **all** lines; today-MOVE lands checked.
Auth `/me`, CSRF, login, register, and logout do not roll. Invalid
archive mutations (`400` / `404`) do not roll.

Forward plan is one dated collection. Week / month / year are display ranges
over `GET /plan/forward` (ISO week Monday–Sunday; calendar month/year), not
stored horizons. `POST /plan/forward` rejects today and past dates; add a
title for today through PlanToday.

Archive and forward `from`/`to` must satisfy `from <= to` and an inclusive span
of at most 366 days. Empty ranges return `200` with an empty list. One shared
cap (`LibraryLimits.MAX_TITLES_PER_DATE`): PlanToday lines, forward items per
`plannedFor` date, and archive events per `watchedOn` date (`400` when a user
add would exceed). Archive add preflight counts existing rows plus
ensure-induced writes on that date (stale PlanToday output and, when the flag
is on, missed forward on that date). Unknown or other-owner ids are `404`.
Stored PlanToday with
`forDate > today` is `409` `plan_date_conflict`. Owner UUID comes from the
authenticated principal only.

Swagger UI: `http://localhost:8080/swagger-ui.html`

Readiness: `GET /actuator/health` (public). Other actuator endpoints are not exposed.

## Constraints

- Plain module `bootRun` requires local PostgreSQL.
- Full-stack `./gradlew dev` requires local PostgreSQL plus `.env.planner-app`.
- One shared local DB; do not wipe it from automated tests.
- Automated tests never use `localhost:5432/watchnest`.
- Schema stability is **not** supported yet: prefer not to drop casually, but on
  migration/checksum errors reset the local DB (or clear Liquibase history) and
  re-apply. Stable migrate/rollback discipline starts only after an explicit
  project decision.
- Default CORS origin: `http://localhost:5173` (`watchnest.frontend.origin`), credentials allowed.
- Passwords are hashed with BCrypt; never returned or logged.
- Hibernate must not create or update schema (`ddl-auto=validate` on `persistent`).
- `:planner-app:test` = domain unit tests plus HTTP MockMvc against ephemeral
  PostgreSQL 18 (Docker / Testcontainers required). It never uses the shared
  local `watchnest` DB.
- CMS sessions are not durable; there is no `cms_session` table in this cut.

## Layout

```text
planner-app/
  src/main/java/dev/watchnest/plannerapp/
    api/
    auth/
    cms/
    catalog/
    config/
    identity/
    library/
    integration/
    persistence/
      jpa/
    security/
  src/main/resources/
    application.properties
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

`:planner-app:test` needs Docker (PostgreSQL 18 Testcontainers for HTTP tests).
`bootRun` requires local PostgreSQL.

Windows:

```powershell
.\gradlew.bat :planner-app:bootRun
```

Default port: `8080`. Default profile: `persistent`.

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
- `treatPlanAsWatched`: `false`
- PlanToday: created on first library request for today; empty until MOVE or add
- forward plan: empty
- watch archive: empty until a day roll flushes checked lines, or a past-day
  archive correction adds a row

## Scope

In scope: auth session/CSRF, durable accounts + library,
per-user isolation, dashboard PlanToday, dated forward plan GET/POST/DELETE,
date-range archive GET, past-only archive POST/PATCH/DELETE, policy update,
`treatPlanAsWatched` library preference, CMS catalog editor API, event publishers, CORS with
credentials, health readiness, Liquibase schema for `user_account` /
`library_profile` / `watch_event` plus `003` owner/date index, `004` PlanToday
/ forward plan, `005` `treat_plan_as_watched`, `006` `cms_account`,
`007` `catalog_title`, and `008` `cms_account.demo`.

Out of scope: calendar grid, catch-up for skipped days, leftover-title return,
moving an archive row between dates, Docker Compose, Kafka producer adapter,
OAuth/email, multi-profile household model, CMS account registration/CRUD,
viewer catalog UI, catalog ids on PlanToday/archive.
