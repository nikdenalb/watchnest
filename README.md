# WatchNest

WatchNest aims to be a watch planner: help people decide what to watch next,
when, and for how long — not only keep a list of titles.

## Current state

`0.3.0` is a runnable product cut with browser auth, per-user libraries, and
durable local PostgreSQL storage on the full-stack `dev` path — not a finished
product.

What works today:

- Register / login (username + password) with HTTP session and CSRF
- Isolated personal library per authenticated user
- Weekday/weekend episode limits, log today’s watches, remaining quota
- Accounts and library data survive backend restart on local native PostgreSQL
  (`persistent` / full-stack `dev`; details in `RELEASES.md` for `0.3.0`)
- Local stack: `identity` + `planner` + Spring Boot API + React SPA

What is not there yet: public deploy, analysis agent / recommendations, family /
collaborative profiles, separate test database / Testcontainers, and richer
planning beyond daily quotas. HTTP session may still reset on restart (re-login);
module tests stay on the in-memory `memory` profile.

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
| `identity` | [`identity/README.md`](identity/README.md) | Accounts / credentials domain |
| `planner` | [`planner/README.md`](planner/README.md) | Domain: profile, watch events, quotas |
| `planner-app` | [`planner-app/README.md`](planner-app/README.md) | Spring Boot REST API + session auth |
| `frontend` | [`frontend/README.md`](frontend/README.md) | React + Vite UI (Gradle orchestrates npm) |

Product releases use SemVer in `RELEASES.md`. Each module keeps its own SemVer
and changelog. The non-detachable root module uses `rootVersion`.
`productVersion=0.3.0` (see `RELEASES.md`).

## Development

Conventions: [`CONTRIBUTING.md`](CONTRIBUTING.md).

Quick start from root (Windows):

```powershell
.\scripts\dev.ps1
```

Or via Gradle wrapper / Unix script — see `ROOT_README.md`.

`dev` starts `planner-app` on port `8080` with profile `persistent` (loads
ignored `.env.planner-app` via `scripts/dev.*`), waits for `GET /actuator/health`,
then starts Vite on `5173`. Requires local native PostgreSQL — see `RELEASES.md`
(`0.3.0`) and `ROOT_README.md`.

- UI: http://localhost:5173
- API: http://localhost:8080
- Readiness: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html

Useful Gradle tasks: `projects`, `build`, `:planner-app:bootRun`,
`:frontend:npmDev` / `npmTest` / `npmBuild`.

Safe API env example (owned by `planner-app`):
`config/examples/planner-app.env.example`.
Local secrets: `secrets/` (gitignored except `secrets/README.md`).

## License

Apache License 2.0.
