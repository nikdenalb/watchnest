# root module

Module README for the non-detachable `root` of WatchNest.

The project/product overview lives in [`README.md`](README.md). That file is
**not** part of this module: update it only with a product release
(`productVersion` + `RELEASES.md`). This file documents **root as a module**.

**Version:** `rootVersion` in `gradle.properties` (see `CHANGELOG.md`).  
**Product version:** `productVersion` in `gradle.properties` (see `RELEASES.md`).

## Role

Top-level project infrastructure shared by all modules:

- Gradle multi-module settings and wrapper
- Root versioning (`rootVersion`, this file, `CHANGELOG.md`)
- Shared Cursor rules under `.cursor/rules/`
- Dev scripts (`scripts/dev.ps1`, `scripts/dev.sh`) and root `dev` Gradle task
- Safe config templates under `config/examples/`
- Contributing conventions (`CONTRIBUTING.md`) and licensing
- Product-release files (`productVersion`, `RELEASES.md`) only in the dedicated
  product-release commit — not mixed into ordinary `rootVersion` infra bumps

`root` is not a deployable service and is not detachable into its own product
repo the way `planner` or `frontend` might be later.

## What lives in root vs a feature module

| In `root` | Not in `root` module commits |
| --- | --- |
| `settings.gradle.kts`, wrapper | feature-module code / version / changelog |
| `rootVersion`, `ROOT_README.md`, `CHANGELOG.md` | top-level `README.md` (product overview) |
| `scripts/`, shared rules, `CONTRIBUTING.md` | `productVersion` + `RELEASES.md` (product-release commit only) |
| `config/examples/` templates, license | |
| `BACKLOG.md` | |

## Related paths

```text
/
  README.md          ← product overview (with productVersion only)
  ROOT_README.md     ← this file (root module README)
  CHANGELOG.md       ← root module changelog
  RELEASES.md        ← product release manifest (with productVersion)
  CONTRIBUTING.md
  settings.gradle.kts
  gradle.properties
  scripts/
  config/examples/
  .cursor/rules/
```

## Modules registered in Gradle

All monorepo modules are Gradle subprojects while they live here:

- [`planner`](planner/README.md) — domain
- [`planner-app`](planner-app/README.md) — Spring Boot API (`→ :planner`)
- [`frontend`](frontend/README.md) — React SPA (Gradle wraps npm/Vite only)

Compile dependency allowed: `planner-app` → `planner`.  
`frontend` talks to `planner-app` over HTTP only.

## Local full-stack start

```bash
./gradlew dev
# Windows: .\gradlew.bat dev
# or scripts directly: ./scripts/dev.sh  /  .\scripts\dev.ps1
```

Order: `:planner-app:bootRun` on **8080** → health `GET /api/v1/dashboard` →
Vite on **5173**. Frontend uses relative `/api` (proxied to the API). CORS default
in `planner-app` is `http://localhost:5173`.

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- UI: `http://localhost:5173`

Env template for the API lives with the `planner-app` module:
`config/examples/planner-app.env.example`.

## Commit rule

Changes under root must be committed alone as `root` (see `CONTRIBUTING.md`).
Do not mix root infrastructure with `planner` / `planner-app` / `frontend` in
one commit.
Do not put top-level `README.md` in a `rootVersion` infra commit; ship it with
the product-release commit (`productVersion` + `RELEASES.md`).
