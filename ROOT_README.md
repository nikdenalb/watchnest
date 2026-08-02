# root

Non-detachable infrastructure module for the WatchNest monorepo.

**Version:** `rootVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- Gradle multi-module settings and wrapper;
- root versioning and changelog;
- shared Cursor rules, contributing docs, public backlog;
- local full-stack start (`dev` task and `scripts/dev.*`);
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
| Templates | `config/examples/` placeholders (module-owned examples may live here) |

## Constraints

- Not a deployable service.
- Does not own feature-module code, versions, or changelogs.
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
  config/examples/
  .cursor/rules/
```

## Registered subprojects

| Module | Docs | Role |
| --- | --- | --- |
| `identity` | [`identity/README.md`](identity/README.md) | Accounts / credentials domain |
| `planner` | [`planner/README.md`](planner/README.md) | Watch-library domain |
| `planner-app` | [`planner-app/README.md`](planner-app/README.md) | Spring Boot API |
| `frontend` | [`frontend/README.md`](frontend/README.md) | React SPA (Gradle wraps npm/Vite) |

Allowed compile dependencies: `planner-app` → `planner`, `planner-app` → `identity`.  
`frontend` talks to `planner-app` over HTTP only.

## Build and run

```bash
./gradlew projects
./gradlew dev
# Windows: .\gradlew.bat dev
# or: ./scripts/dev.sh  /  .\scripts\dev.ps1
```

`dev`: `:planner-app:bootRun` on **8080** → wait for `GET /actuator/health` →
Vite on **5173**. SPA uses relative `/api` (proxied to the API).

| Surface | URL |
| --- | --- |
| API | `http://localhost:8080` |
| Readiness | `http://localhost:8080/actuator/health` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| UI | `http://localhost:5173` |

API env template: `config/examples/planner-app.env.example`.

## Scope

In scope: multi-module registration (including `identity`), local full-stack
`dev` orchestration, shared rules and root docs.

Out of scope: feature behavior inside `identity` / `planner` / `planner-app` /
`frontend`; product release notes (see `RELEASES.md`).
