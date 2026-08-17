# WatchNest

WatchNest is a watch planner: what to watch next, when, and for how long —
personal library first, smarter planning as it grows.

## Snapshot

Early personal product: one person, one library, a daily episode quota, and a
diary of watches. Not a family planner and not a production SaaS.

| | |
| --- | --- |
| Demo | [http://158.160.223.53](http://158.160.223.53) — HTTP, no custom domain, no SLA |
| Guest | `alice` / `12345678` — shared demo library; anyone can write to it |
| CD | Test environment: follows green `dev`, so it can be ahead of a product cut, or down. |

### Product line

| Version | What landed |
| --- | --- |
| `0.1.0` | Runnable personal library across modules |
| `0.2.0` | Browser register/login and per-user isolation |
| `0.3.0` | Durable accounts and library on PostgreSQL |
| `0.4.0` | Public demo on a Yandex Cloud VM |
| `0.5.0` | Watch-history archive (browse by day) |
| `0.6.0` | PlanToday and a dated forward plan |

Patch cuts (`0.4.1`, `0.5.1`, `0.5.2`) are in `RELEASES.md`.

### Stack

Java 25, Spring Boot 3.5, React + Vite (Node 24), PostgreSQL 18, Gradle,
GitHub Actions, Docker Compose on one Yandex Cloud VM.

## Current state

`0.6.0` replaces log-today with PlanToday and a dated forward plan. Same-day
titles live on today’s plan until the day rolls into the archive. The product
is still browser auth, per-user libraries, durable PostgreSQL, and a public
demo on a Yandex Cloud VM that updates from green `dev` CI — not a finished
product.

What works today:

- Register / login (username + password) with HTTP session and CSRF
- Isolated personal library per authenticated user
- Weekday/weekend episode limits; quota counts PlanToday lines
- PlanToday: plan and check titles for the working day
- Dated forward plan; week/month/year are display ranges, not a calendar grid
- Watch history by day: browse past months as a diary list
- Accounts, library, and plans survive backend and VM reboot on PostgreSQL
- Local stack: `identity` + `planner` + Spring Boot API + React SPA
- Demo stack: Docker Compose under `deploy/` (Postgres, API, nginx) on a Yandex
  Cloud VM ([http://158.160.223.53](http://158.160.223.53)); CD test environment
  on green `dev`, so it can be ahead of `RELEASES.md` or down
- CI on `dev` and pull requests; a green push to `dev` publishes private GHCR
  images and deploys that SHA to the demo VM. Fast-forward of `main` does not
  re-run the suite.

Some deferred ideas are in `BACKLOG.md`; that list is not a full plan.

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
| `planner` | [`planner/README.md`](planner/README.md) | Domain: profile, plans, watch events, quotas |
| `planner-app` | [`planner-app/README.md`](planner-app/README.md) | Spring Boot REST API + session auth |
| `frontend` | [`frontend/README.md`](frontend/README.md) | React + Vite UI (Gradle orchestrates npm) |

Product releases use SemVer in `RELEASES.md`. Each module keeps its own SemVer
and changelog. The non-detachable root module uses `rootVersion`.
`productVersion=0.6.0` (see `RELEASES.md`).

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
