# WatchNest

WatchNest aims to be a watch planner: help people decide what to watch next,
when, and for how long — not only keep a list of titles.

## Current state

`0.1.0` is the first runnable product cut, not a finished product.

What works today:

- Personal library profile with weekday/weekend episode limits
- Log today’s watches and see remaining quota
- Local stack: domain module + Spring Boot API + React SPA (`./gradlew dev`)
- In-memory API state (resets when the backend restarts)

What is not there yet: durable storage, public deploy, analysis agent, family /
collaborative profiles, and richer planning beyond daily quotas.

## Plan

Build toward a planner that combines:

- **Planning and screen-time control** — quotas, remaining time, and schedules
  that respect limits instead of endless browse-and-play.
- **An analysis agent** — use watch history, reviews, ratings, and stated
  wishes to suggest titles and plans for a person or household.
- **Family and collaborative viewing** — shared sessions and plans that balance
  several people’s tastes, constraints, and preferences.

This repository is a Gradle multi-module monorepo. `frontend` is included in
Gradle as a thin orchestration module that still builds with npm/Vite.

## Modules

| Module | Docs | Role |
| --- | --- | --- |
| `root` | [`ROOT_README.md`](ROOT_README.md) | Project infrastructure (Gradle, scripts, rules, release files) |
| `planner` | [`planner/README.md`](planner/README.md) | Domain: profile, watch events, quotas |
| `planner-app` | [`planner-app/README.md`](planner-app/README.md) | Spring Boot REST API |
| `frontend` | [`frontend/README.md`](frontend/README.md) | React + Vite UI (Gradle orchestrates npm) |

Product releases use SemVer in `RELEASES.md`. Each module keeps its own SemVer
and changelog. The non-detachable root module uses `rootVersion`.
`productVersion=0.1.0` (see `RELEASES.md`).

## Development

Conventions: [`CONTRIBUTING.md`](CONTRIBUTING.md).

Quick start from root:

```bash
./gradlew dev
# or: ./scripts/dev.sh
# Windows: .\scripts\dev.ps1  /  .\gradlew.bat dev
```

`dev` starts `planner-app` on port `8080`, waits for `GET /api/v1/dashboard`, then
starts Vite on `5173`. The SPA calls relative `/api/v1/*` (Vite proxies `/api` →
`8080`). Backend CORS default origin is `http://localhost:5173`.

- UI: http://localhost:5173
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

Useful Gradle tasks:

```bash
./gradlew projects
./gradlew build
./gradlew :planner-app:bootRun
./gradlew :frontend:npmDev
./gradlew :frontend:npmTest
./gradlew :frontend:npmBuild
```

Safe API env example (owned by `planner-app`):
`config/examples/planner-app.env.example`.
Local secrets: `secrets/` (gitignored except `secrets/README.md`).

## License

Apache License 2.0.
