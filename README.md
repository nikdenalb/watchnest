# WatchNest

WatchNest is a watch planner: what to watch next, when, and for how long —
personal library first, smarter planning as it grows.

## Snapshot

Early personal product: one person, one library, a daily episode quota, and a
diary of watches. Not a family planner and not a production SaaS.

| | |
| --- | --- |
| Demo | [http://158.160.223.53](http://158.160.223.53) — HTTP, no custom domain, no SLA |
| Guest | `alice` / `12345678` — shared **viewer** library; anyone can write to it. |
| CMS | [http://158.160.223.53/cms/](http://158.160.223.53/cms/) — catalog editor; sign-in only, accounts provisioned out of band |
| CMS demo | `editor` / `cmsdemo1` — shared **CMS** catalog editor; demonstration account, catalog writes are not saved. |
| CD | Test environment: follows green `dev`, so it can be ahead of a product cut, or down. |

### Цель

Проект служит инженерной практикой полного цикла: доменная модель, API,
данные, CI и рабочий деплой. Фронтенд целиком собран вайбкодингом.

WatchNest — органайзер просмотра видео. Это среда планирования будущих
просмотров: история, планы, оценки, ограничения по времени и собственный
каталог живут в приложении.
Данные этого контура — потенциальный материал для агента при построении
рекомендаций.

Следующий этап — исследование персональных рекомендаций и сигналов,
которые делают их полезнее. Одна из гипотез — неопубликованные черновики
рецензий как сигнал предпочтений.

### Product line

| Version | What landed |
| --- | --- |
| `0.1.0` | Runnable personal library across modules |
| `0.2.0` | Browser register/login and per-user isolation |
| `0.3.0` | Durable accounts and library on PostgreSQL |
| `0.4.0` | Public demo on a Yandex Cloud VM |
| `0.5.0` | Watch-history archive (browse by day) |
| `0.6.0` | PlanToday and a dated forward plan |
| `0.7.0` | Correct past watch-history days |
| `0.8.0` | Opt-in: treat planned titles as watched |
| `0.9.0` | Owned title catalog and closed `/cms/` editor |
| `0.10.0` | Demonstration CMS accounts; public CMS demo login |
| `0.11.0` | PostgreSQL-only API runtime; Docker required for JVM tests |

Patch cuts (`0.4.1`, `0.5.1`, `0.5.2`, `0.8.1`) are in `RELEASES.md`.

### Stack

Java 25, Spring Boot 3.5, React + Vite (Node 24), PostgreSQL 18, Gradle,
GitHub Actions, Docker Compose on one Yandex Cloud VM.

## Current state

`0.11.0` drops the in-memory planner-app runtime. Local `bootRun` and
`./gradlew dev` need native PostgreSQL; `./gradlew test` needs Docker
(Testcontainers PostgreSQL 18). The public demo is unchanged.
The owned title catalog and closed `/cms/` editor remain. Demonstration CMS
accounts can sign in and read titles, but catalog writes are not saved. The public CMS demo login is `editor` / `cmsdemo1` (same
idea as the viewer guest). CMS sign-in stays separate from the viewer;
accounts are provisioned out of band (no CMS registration). The viewer guest
`alice` cannot open the editor.
Viewer PlanToday and archive still use free-text titles — they are not linked
to catalog ids. The opt-in treat-planned-as-watched setting stays under the
username in the session bar. Default stays PlanToday checkboxes; unchecked
lines and missed forward are discarded on roll. When the setting is on, titles
left on PlanToday and dated plans for missed days go to watch history when the
day rolls — missed days archive only if that flag was already on at roll.
Same-day titles still live on PlanToday until then. Forward-plan add starts
tomorrow. Past diary days are still correctable behind gears. Login handles
are canonical lowercase. The product is still browser auth, per-user
libraries, durable PostgreSQL, and a public demo on a Yandex Cloud VM that
updates from green `dev` CI — not a finished product.

What works today:

- Register / login (username + password) with HTTP session and CSRF
- Isolated personal library per authenticated user
- Weekday/weekend episode limits; quota counts PlanToday lines
- PlanToday: plan and check titles for the working day (default)
- Username menu: opt-in “treat planned titles as watched” and Log out (no
  PlanToday checkboxes when the flag is on; Remove anything not watched)
- Dated forward plan; week/month/year are display ranges, not a calendar grid
- Watch history by day: browse past months as a diary; correct past days
  behind gears (add, rename, delete); missed forward can archive on roll when
  the Account setting is already on
- Closed catalog editor at `/cms/`: sign-in, search/list/create/edit/delete
  titles (`FILM`, `TV_SERIES`, `MINI_SERIES`, `TV_SHOW`). Demonstration
  accounts see the same controls; blocked writes show an alert and do not
  change the catalog
- Public CMS demo on the Yandex VM: `editor` / `cmsdemo1`
- Accounts, library, plans, and catalog rows survive backend and VM reboot on
  PostgreSQL (CMS login does not; restart logs the editor out)
- Local stack: `identity` + `planner` + `catalog` + Spring Boot API + viewer
  SPA + CMS SPA
- Demo stack: Docker Compose under `deploy/` (Postgres, API, nginx) on a Yandex
  Cloud VM ([http://158.160.223.53](http://158.160.223.53), CMS at
  [http://158.160.223.53/cms/](http://158.160.223.53/cms/)); CD test environment
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
| `catalog` | [`catalog/README.md`](catalog/README.md) | Title catalog domain |
| `planner-app` | [`planner-app/README.md`](planner-app/README.md) | Spring Boot REST API + session auth + CMS API |
| `frontend` | [`frontend/README.md`](frontend/README.md) | Viewer React + Vite UI (Gradle orchestrates npm) |
| `cms` | [`cms/README.md`](cms/README.md) | Catalog editor SPA (Gradle orchestrates npm) |

Product releases use SemVer in `RELEASES.md`. Each module keeps its own SemVer
and changelog. The non-detachable root module uses `rootVersion`.
`productVersion=0.11.0` (see `RELEASES.md`).

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
- CMS (second terminal `:cms:npmDev`): http://localhost:5174/cms/
- API: http://localhost:8080
- Readiness: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html

Useful Gradle tasks: `projects`, `build`, `:planner-app:bootRun`,
`:frontend:npmDev` / `npmTest` / `npmBuild`, `:cms:npmDev` / `npmTest` /
`npmBuild`.

Safe API env example (owned by `planner-app`):
`config/examples/planner-app.env.example`.
Local secrets: `secrets/` (gitignored except `secrets/README.md`).

## License

Apache License 2.0.
