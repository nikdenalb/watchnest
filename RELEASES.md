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
