# WatchNest

WatchNest is a watch planner: what to watch next, when, and for how long —
personal library first, smarter planning as it grows.

## Current state

`0.5.0` is a runnable product cut with browser auth, per-user libraries, a
watch-history archive, durable PostgreSQL, and a public demo on a Yandex Cloud
VM that updates from green `dev` CI — not a finished product.

What works today:

- Register / login (username + password) with HTTP session and CSRF
- Isolated personal library per authenticated user
- Weekday/weekend episode limits, log today’s watches, remaining quota
- Watch history by day: browse past months as a diary list (not only today)
- Accounts and library data survive backend and VM reboot on PostgreSQL
- Local stack: `identity` + `planner` + Spring Boot API + React SPA
- Demo stack: Docker Compose under `deploy/` (Postgres, API, nginx) on a Yandex
  Cloud VM (HTTP on the VM public IP; see `RELEASES.md` for `0.5.0`)
- CI on `dev` and `main`; a green push to `dev` publishes private GHCR images
  and deploys that SHA to the demo VM

Next: HTTP tests against real PostgreSQL (Testcontainers, not the shared local
database). Custom domain and HTTPS for the public demo stay in `BACKLOG.md`.

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
`productVersion=0.5.0` (see `RELEASES.md`).

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
and `ROOT_README.md`.

Demo packaging: [`deploy/README.md`](deploy/README.md).

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
