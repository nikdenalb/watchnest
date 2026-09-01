# Releases

Product release manifest for WatchNest.

Product releases use SemVer: `MAJOR.MINOR.PATCH`.
The current product release version is stored in `gradle.properties` as `productVersion`.
The value `0.0.0` means that the product has not been released yet.

Each detachable module keeps its own version and changelog inside the module directory.
This file records which module versions are included in each product release.

Versioning rules:

- `MAJOR`: incompatible product-level changes.
- `MINOR`: product-level features or notable module additions.
- `PATCH`: product-level fixes and maintenance releases.

## [0.11.0] - 2026-09-01

PostgreSQL is the only planner-app runtime; local JVM tests use Testcontainers.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.4.1 |
| `identity` | 0.1.0 |
| `planner` | 0.3.0 |
| `planner-app` | 0.10.1 |
| `catalog` | 0.1.0 |
| `cms` | 0.2.3 |
| `frontend` | 0.7.1 |

### Product scope

- Everything in `0.10.0` (demonstration CMS accounts and public CMS demo login)
- One planner-app runtime: PostgreSQL + Liquibase + JPA (no in-memory `memory`
  profile)
- Local `bootRun` / `./gradlew dev` require native PostgreSQL
- `./gradlew test` includes planner-app HTTP suites against ephemeral
  PostgreSQL 18 (Docker / Testcontainers). CI JVM step is the same
  `./gradlew test`
- The public demo is unchanged (already PostgreSQL)

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- CMS sign-in uses an in-memory token; process restart logs CMS users out
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local `bootRun` needs PostgreSQL; JVM `test` needs Docker
- Day roll is the next library request after server `today` changes
- Setting off: unchecked PlanToday lines and missed dated entries are discarded
- Turning the flag on after a false-roll does not resurrect expired items
- No catch-up for skipped days; no moving a watch to another date
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- Leftover forward items with `plannedFor` today or earlier are read-only
- No viewer catalog UI; plans are not catalog ids
- No CMS account registration, password-change, or user-management UI
- CD deploys `dev` only

## [0.10.0] - 2026-08-30

Demonstration CMS accounts and a public CMS demo login.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.4.0 |
| `identity` | 0.1.0 |
| `planner` | 0.3.0 |
| `planner-app` | 0.9.2 |
| `catalog` | 0.1.0 |
| `cms` | 0.2.3 |
| `frontend` | 0.7.1 |

### Product scope

- Everything in `0.9.0` (owned title catalog and closed `/cms/` editor)
- A CMS account may be marked demonstration (`cms_account.demo`). It can sign
  in and read titles. Create, update, and delete are rejected with `403`
  `demo_account`; the catalog is unchanged
- Public demo CMS login on the Yandex VM (see top-level `README.md`), in the
  same style as the viewer guest `alice`. Catalog writes from that account are
  not saved
- CMS accounts remain provisioned out of band; there is still no CMS
  registration or account-management API
- CMS editor fetches a fresh CSRF token immediately before each unsafe request
  (`cms` 0.2.2 / 0.2.3); leftover blank CMS CSRF cookies are ignored
  (`planner-app` 0.9.2)

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- CMS sign-in uses an in-memory token; process restart logs CMS users out
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Day roll is the next library request after server `today` changes
- Setting off: unchecked PlanToday lines and missed dated entries are discarded
- Turning the flag on after a false-roll does not resurrect expired items
- No catch-up for skipped days; no moving a watch to another date
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- Leftover forward items with `plannedFor` today or earlier are read-only
- No viewer catalog UI; plans are not catalog ids
- No CMS account registration, password-change, or user-management UI
- CD deploys `dev` only

## [0.9.0] - 2026-08-27

Owned title catalog and closed CMS editor.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.4.0 |
| `identity` | 0.1.0 |
| `planner` | 0.3.0 |
| `planner-app` | 0.8.1 |
| `catalog` | 0.1.0 |
| `cms` | 0.1.0 |
| `frontend` | 0.7.0 |

### Product scope

- Everything in `0.8.1` (account settings under the username)
- Owned title catalog (`FILM`, `TV_SERIES`, `MINI_SERIES`, `TV_SHOW`); no
  Kinopoisk / TMDB / OMDb fetch
- Closed catalog editor at `/cms/` with a separate CMS sign-in (not the
  viewer guest)
- CMS accounts provisioned out of band; no CMS registration or account-management
  API
- Viewer PlanToday and archive still use free-text titles; they are not linked
  to catalog ids

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- CMS sign-in uses an in-memory token; process restart logs CMS users out
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Day roll is the next library request after server `today` changes
- Setting off: unchecked PlanToday lines and missed dated entries are discarded
- Turning the flag on after a false-roll does not resurrect expired items
- No catch-up for skipped days; no moving a watch to another date
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- Leftover forward items with `plannedFor` today or earlier are read-only
- No viewer catalog UI; plans are not catalog ids
- No CMS account registration, password-change, or user-management UI
- CD deploys `dev` only

## [0.8.1] - 2026-08-21

Account settings under the username.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.10 |
| `identity` | 0.1.0 |
| `planner` | 0.3.0 |
| `planner-app` | 0.7.0 |
| `frontend` | 0.7.0 |

### Product scope

- Everything in `0.8.0` (opt-in treat planned titles as watched)
- `treatPlanAsWatched` and Log out live in a session-bar disclosure under the
  username; the dashboard Account card is gone
- Login handles stay canonical lowercase

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Day roll is the next library request after server `today` changes
- Setting off: unchecked PlanToday lines and missed dated entries are discarded
- Turning the flag on after a false-roll does not resurrect expired items
- No catch-up for skipped days; no moving a watch to another date
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- Leftover forward items with `plannedFor` today or earlier are read-only
- CD deploys `dev` only

## [0.8.0] - 2026-08-20

Opt-in treat planned titles as watched.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.10 |
| `identity` | 0.1.0 |
| `planner` | 0.3.0 |
| `planner-app` | 0.7.0 |
| `frontend` | 0.6.0 |

### Product scope

- Everything in `0.7.0` (past-day archive correction, PlanToday, dated forward
  plan, durable PostgreSQL, Compose demo)
- Account setting `treatPlanAsWatched` (default **off**): PlanToday is the
  watch log; titles left there and dated plans for missed days archive on roll
- `PUT /api/v1/library-preferences`; dashboard JSON includes the flag
- Missed forward archives **only** if that flag was already on at roll

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Day roll is the next library request after server `today` changes
- Setting off: unchecked PlanToday lines and missed dated entries are discarded
- Turning the flag on after a false-roll does not resurrect expired items
- No catch-up for skipped days; no moving a watch to another date
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- Leftover forward items with `plannedFor` today or earlier are read-only
- CD deploys `dev` only

## [0.7.0] - 2026-08-18

Past-day archive correction; forward-plan add no longer targets today.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.10 |
| `identity` | 0.1.0 |
| `planner` | 0.2.0 |
| `planner-app` | 0.6.0 |
| `frontend` | 0.5.0 |

### Product scope

- Everything in `0.6.0` (PlanToday, dated forward plan, archive GET, durable
  PostgreSQL, Compose demo, CI HTTP tests on PostgreSQL)
- Past-day archive add, rename, and delete (`watchedOn` before server today)
- Correction is behind gears and overlay dialogs; the month diary stays a list
- `POST /api/v1/watch-events` is past-only; `PATCH` title and `DELETE` for past rows
- Forward-plan add starts at tomorrow and never posts PlanToday

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Day roll is the next library request after server `today` changes
- Unchecked PlanToday lines and missed dated entries are discarded
- No catch-up for skipped days; no moving a watch to another date
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- Leftover forward items with `plannedFor` today or earlier are read-only
- CD deploys `dev` only

## [0.6.0] - 2026-08-17

PlanToday replaces log-today; dated forward plan; archive flush on day roll.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.10 |
| `identity` | 0.1.0 |
| `planner` | 0.2.0 |
| `planner-app` | 0.5.0 |
| `frontend` | 0.4.0 |

### Product scope

- Everything in `0.5.2` (session auth, per-user library, watch-history archive,
  durable PostgreSQL, Compose demo, Node 24, CI HTTP tests on PostgreSQL)
- Working day is PlanToday: add/check/remove lines; quota counts those lines
- Dated forward plan; week/month/year are display ranges over one list
- Entries for today MOVE onto PlanToday; missed dates are discarded
- Checked lines become archive `WatchEvent`s when the day rolls (`watchedOn` =
  the closed PlanToday date)
- `POST /api/v1/watch-events` is removed; archive GET stays

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Day roll is the next library request after server `today` changes
- Unchecked PlanToday lines and missed dated entries are discarded
- No catch-up for skipped days; no edit/delete of archive rows
- Archive UI is a day list; forward plan is a dated list, not a calendar grid
- CD deploys `dev` only

## [0.5.2] - 2026-08-16

CI HTTP coverage against ephemeral PostgreSQL; skip a second suite when
fast-forwarding `main` from a green `dev` SHA.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.10 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.4.1 |
| `frontend` | 0.3.1 |

### Product scope

- Everything in `0.5.1` (session auth, per-user library, watch-history archive,
  durable PostgreSQL, Compose demo, Node 24 toolchain)
- `planner-app` HTTP tests on PostgreSQL 18 via Testcontainers in CI
  (`:planner-app:persistentHttpTest`); local `./gradlew test` stays Docker-free
- GitHub Actions: tests on `dev` and pull requests; push to `main` does not
  re-run the suite after a fast-forward of an already-green SHA

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Local module `test` / plain `bootRun` still use the in-memory `memory` profile
- Cannot log a watch on a past date; no edit/delete of archive rows
- Archive UI is a day list, not a week/month/year calendar grid
- CD deploys `dev` only

## [0.5.1] - 2026-08-15

SPA toolchain on Node 24 LTS; GitHub Actions majors that declare Node 24.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.9 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.4.0 |
| `frontend` | 0.3.1 |

### Product scope

- Everything in `0.5.0` (session auth, per-user library, watch-history archive,
  durable PostgreSQL, Compose demo, CI/CD from green `dev`)
- Frontend `engines` / `.nvmrc` / CI npm / web image: Node 24 LTS
- Actions: `checkout@v7`, `setup-java@v5`, `setup-node@v7`, docker buildx/login
  `@v4`, `build-push-action@v7`

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Module tests / plain `bootRun` still use in-memory `memory` profile (no DB)
- Cannot log a watch on a past date; no edit/delete of archive rows
- Archive UI is a day list, not a week/month/year calendar grid
- CD deploys `dev` only

## [0.5.0] - 2026-08-14

Watch history: logged watches are browsable by day, not only “today”.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.8 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.4.0 |
| `frontend` | 0.3.0 |

### Product scope

- Everything in `0.4.1` (session auth, per-user library, durable PostgreSQL,
  Compose demo, CI/CD from green `dev`)
- `GET /api/v1/watch-events?from&to` — inclusive range, max 366 days
- Dashboard Watch history: month diary list, bounds from server `today`
- Logging a watch still stamps server today and appears in the current month

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Module tests / plain `bootRun` still use in-memory `memory` profile (no DB)
- Cannot log a watch on a past date; no edit/delete of archive rows
- Archive UI is a day list, not a week/month/year calendar grid
- CD deploys `dev` only

## [0.4.1] - 2026-08-13

Demo delivery: the `0.4.0` product still runs on the Yandex VM; a green push
to `dev` now tests, publishes private GHCR images, and deploys that SHA
without building on the host. No new application features.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.8 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.3.0 |
| `frontend` | 0.2.0 |

### Product scope

- Everything in `0.4.0` (session auth, per-user library, durable PostgreSQL,
  Compose demo on the Yandex VM)
- GitHub Actions: full JVM + frontend tests on pull requests and pushes to
  `dev` and `main`
- Private GHCR images `watchnest-app` / `watchnest-web` tagged by git SHA
- Demo VM updates from green `dev` (pull + `up --no-build`); `.env.demo`
  stays on the host

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Module tests / plain `bootRun` still use in-memory `memory` profile (no DB)
- CD deploys `dev` only; `main` is PR-gated for reviewed product cuts

## [0.4.0] - 2026-08-12

Public demo deploy: the 0.3.0 product runs on a Yandex Cloud VM via Docker
Compose; accounts and libraries survive VM reboot.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.4 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.3.0 |
| `frontend` | 0.2.0 |

### Product scope

- Everything in `0.3.0` (session auth, per-user library, durable PostgreSQL)
- Docker Compose packaging under `deploy/` (Postgres 18, `planner-app`, nginx)
- Demo host on a Yandex Cloud VM; HTTP on the VM public IP
- Data survives full VM reboot (re-login required; library data remains)

### Known limits

- HTTP session may reset on restart (re-login required); data remains in PG
- Single-VM demo (no SLA, no managed ALB / Yandex Certificate Manager yet)
- Module tests / plain `bootRun` still use in-memory `memory` profile (no DB)

## [0.3.0] - 2026-08-09

Durable local storage: accounts and personal libraries survive backend restart on
PostgreSQL when running the full-stack `dev` path (`persistent` profile).

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.2 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.3.0 |
| `frontend` | 0.2.0 |

### Product scope

- Username/password registration and login (HTTP session + CSRF)
- Each authenticated user has an isolated personal library (profile, policy, watches)
- Personal library: weekday/weekend episode limits, log today’s watches, remaining quota
- Durable accounts and library on **local native PostgreSQL** (Docker Compose not
  part of this cut): default `jdbc:postgresql://localhost:5432/watchnest`,
  profile `persistent`, full-stack `dev` via root scripts
- Verified against **PostgreSQL 18**; other 16+ majors may work but are unproven
  in this release
- Liquibase-owned schema; JPA adapters in `planner-app` only
- React SPA + Spring Boot API + `planner` / `identity` domain libraries
- Local full-stack start via Gradle/`scripts` `dev` (readiness: `/actuator/health`)

### Known limits

- HTTP session may reset on backend restart (re-login required); data remains in PG
- Module tests / plain `bootRun` still use in-memory `memory` profile (no DB)
- One shared local database for development (no separate test DB / Testcontainers yet)
- No household / multi-profile yet
- No analysis agent / recommendations engine yet
- No public deploy in this cut

## [0.2.0] - 2026-08-02

Browser personalization: register/login, per-user library isolation, and session
auth across API and SPA.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.3.1 |
| `identity` | 0.1.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.2.0 |
| `frontend` | 0.2.0 |

### Product scope

- Username/password registration and login (HTTP session + CSRF)
- Each authenticated user has an isolated personal library (profile, policy, watches)
- Personal library: weekday/weekend episode limits, log today’s watches, remaining quota
- React SPA (dark theme) + Spring Boot API + `planner` / `identity` domain libraries
- Local full-stack start via `./gradlew dev` (readiness: `/actuator/health`)

### Known limits

- In-memory accounts and library state (reset on backend restart)
- No durable database or cloud sync
- No household / multi-profile yet
- No analysis agent / recommendations engine yet
- No public deploy in this cut

## [0.1.0] - 2026-07-31

First product cut: a runnable personal watch library across all modules.

### Included module versions

| Module | Version |
| --- | --- |
| `root` | 0.2.0 |
| `planner` | 0.1.0 |
| `planner-app` | 0.1.0 |
| `frontend` | 0.1.0 |

### Product scope

- Personal library profile with weekday/weekend episode limits
- Log today’s watches and see remaining quota
- React SPA (dark theme) + Spring Boot API + domain module
- Local full-stack start via `./gradlew dev`

### Known limits

- In-memory API state (resets on backend restart)
- No household / multi-profile yet
- No durable database
- No public deploy in this cut
- No authentication (single shared in-process library)
