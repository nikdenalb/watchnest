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
