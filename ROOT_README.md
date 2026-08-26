# root

Non-detachable infrastructure module for the WatchNest monorepo.

**Version:** `rootVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- Gradle multi-module settings and wrapper;
- root versioning and changelog;
- shared Cursor rules, contributing docs, `BACKLOG.md`;
- local full-stack start (`dev` task and `scripts/dev.*`);
- Docker Compose packaging under `deploy/` for the public site (viewer + CMS);
- safe config templates under `config/examples/`.

Project/product overview: [`README.md`](README.md). Product release manifest:
[`RELEASES.md`](RELEASES.md).

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Settings | `settings.gradle.kts`: register subprojects |
| Build | root `build.gradle.kts`, wrapper, shared Java test defaults |
| Versions | `rootVersion` in `gradle.properties`; root `CHANGELOG.md` |
| Docs | this file, `CONTRIBUTING.md`, `BACKLOG.md` |
| Rules | shared `.cursor/rules/*.mdc` (not `*.local.mdc`) |
| Dev run | root `dev` task → `scripts/dev.ps1` / `scripts/dev.sh` |
| Deploy pack | `deploy/` Docker Compose (app + Postgres + nginx) |
| Templates | `config/examples/` placeholders (module-owned examples may live here) |

## Constraints

- Not a deployable service.
- Does not own feature modules (their code, versions, or changelogs).
- Commit and release conventions: [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Layout

```text
/
  ROOT_README.md
  CHANGELOG.md
  CONTRIBUTING.md
  BACKLOG.md
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  scripts/
  deploy/
  config/examples/
  .cursor/rules/
```

## Registered subprojects

| Module | Docs | Role |
| --- | --- | --- |
| `identity` | [`identity/README.md`](identity/README.md) | Accounts / credentials domain |
| `planner` | [`planner/README.md`](planner/README.md) | Watch-library domain |
| `catalog` | [`catalog/README.md`](catalog/README.md) | Title catalog domain |
| `planner-app` | [`planner-app/README.md`](planner-app/README.md) | Spring Boot API |
| `frontend` | [`frontend/README.md`](frontend/README.md) | Viewer React SPA (Gradle wraps npm/Vite) |
| `cms` | [`cms/README.md`](cms/README.md) | Catalog editor SPA (Gradle wraps npm/Vite) |

Allowed compile dependencies: `planner-app` → `planner`, `planner-app` → `identity`,
`planner-app` → `catalog`.
`frontend` talks to `planner-app` over HTTP at `/api/v1`.
`cms` talks to `planner-app` over HTTP at `/cms/api/v1`.

## Build and run (local development)

```bash
./gradlew projects
./gradlew dev
# Windows: .\gradlew.bat dev
# or: ./scripts/dev.sh  /  .\scripts\dev.ps1
```

`dev` loads ignored `.env.planner-app` (if present), runs `:planner-app:bootRun`
with Spring profile **`persistent`** (PostgreSQL) on **8080**, waits for
`GET /actuator/health`, then starts the viewer Vite server on **5173**.
It does not start the CMS Vite server.

CMS local UI (second terminal, after the API is up):

```bash
./gradlew :cms:npmDev
# Windows: .\gradlew.bat :cms:npmDev
```

Prerequisites for local `dev`:

1. Local native PostgreSQL running and accepting connections.
2. `.env.planner-app` at repo root — copy from
   `config/examples/planner-app.env.example` and set real `SPRING_DATASOURCE_*`
   values (file is gitignored).
3. Node 24 LTS for Vite (`frontend` and `cms`; see those modules' READMEs).

If `.env.planner-app` is missing, the script warns and still defaults
`SPRING_PROFILES_ACTIVE=persistent`; boot will fail until datasource env is set
and PostgreSQL accepts connections.

| Surface | URL |
| --- | --- |
| API | `http://localhost:8080` |
| Readiness | `http://localhost:8080/actuator/health` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| Viewer UI | `http://localhost:5173` |
| CMS UI | `http://localhost:5174/cms/` |

API env template: `config/examples/planner-app.env.example`.  
Module runtime notes: [`planner-app/README.md`](planner-app/README.md).

## Tests

```bash
./gradlew test
```

Local JVM `test` needs no Docker. GitHub Actions on `dev` and pull requests
also run `:planner-app:persistentHttpTest` against ephemeral PostgreSQL 18.
That gate also runs viewer and CMS unit tests and production builds
(`npm test` then `npm run build` in `frontend/` and in `cms/`) before image
publishing.
That task is not part of `./gradlew build`. Fast-forward of `main` from a
green `dev` SHA does not re-run the suite.

## Public deploy (Docker Compose)

Compose packaging lives under [`deploy/`](deploy/): Postgres 18 + `planner-app`
+ nginx (viewer SPA, CMS SPA under `/cms/`, `/api` and `/cms/api` proxies).
First demo cut uses the VM **public IP** over HTTP; custom domain and HTTPS are
backlog items.

```bash
cp config/examples/watchnest-demo.env.example .env.demo
# edit secrets + VM public IP in ORIGIN / SERVER_NAME
docker compose --env-file .env.demo -f deploy/compose.yaml up -d --build
```

Details: [`deploy/README.md`](deploy/README.md). Pushes to `dev` that pass CI
publish private `app`/`web` images to GHCR and deploy that SHA to the demo VM.

## Scope

In scope: multi-module registration (including `identity`, `catalog`, and
`cms`), local full-stack `dev` orchestration (viewer), Compose packaging under
`deploy/`, shared rules and root docs.

Out of scope: feature behavior inside `identity` / `planner` / `catalog` /
`planner-app` / `frontend` / `cms`; product release notes (see `RELEASES.md`).
