# Root Changelog

Changelog for the non-detachable `root` module.

Product releases are listed in `RELEASES.md`.

## [0.3.7] - 2026-08-12

### Added

- Publish `app` and `web` images to GHCR from GitHub Actions after green CI on
  `dev` (`linux/amd64`, tag = git SHA); add Compose overlay `compose.ghcr.yaml`
  for the later pull-based CD path

## [0.3.6] - 2026-08-12

### Fixed

- Mark `gradlew` executable in git and `chmod +x` it in CI so Linux runners
  can run JVM tests (exit 126 / Permission denied)

## [0.3.5] - 2026-08-12

### Added

- GitHub Actions workflow that runs full JVM tests and frontend unit tests on
  pull requests and pushes to `dev` and `main`

## [0.3.4] - 2026-08-11

### Fixed

- Point the Compose Postgres data volume at `/var/lib/postgresql` (required by
  `postgres:18`); the previous `/var/lib/postgresql/data` mount left `db`
  restarting and blocked `app` / `web` from starting

## [0.3.3] - 2026-08-10

### Added

- Docker Compose packaging under `deploy/` (Postgres, `planner-app`, nginx +
  certbot) for a public demo on a Yandex Cloud VM (HTTP on the VM public IP;
  custom domain optional later)
- Safe demo env template `config/examples/watchnest-demo.env.example`
- `.dockerignore` for Compose image build contexts
- Public backlog item to move TLS to Yandex Certificate Manager + ALB

### Changed

- `ROOT_README.md` documents local `dev` vs Compose deploy under `deploy/`

## [0.3.2] - 2026-08-09

### Changed

- Full-stack `scripts/dev.*` load ignored `.env.planner-app` and default to
  Spring profile `persistent` (local PostgreSQL)
- `ROOT_README.md` documents the PostgreSQL prerequisite for `dev`
- Secret-handling rule and `secrets/README.md` clarify local env vs example
  placeholders
- `.cursor/rules/local/README.md` restates that local rules stay private
  (location marker only; no personal workflow text in git)

## [0.3.1] - 2026-08-02

### Changed

- Product-release commits use custom message form `release(X.Y.Z): …`
  (`commit-naming`, `module-commit-boundaries`, `CONTRIBUTING`)

## [0.3.0] - 2026-08-02

### Added

- Register Gradle subproject `identity`
- Public backlog note deferring a standalone `auth` module/service
- Auth-ready local run: `scripts/dev.*` wait on `GET /actuator/health`

### Changed

- `ROOT_README.md` rewritten as a module-style README; topology includes `identity`
- IntelliJ Gradle module list includes `identity`
- Shared Cursor rules for versioning, commit boundaries, and module README scope

## [0.2.1] - 2026-07-31

### Changed

- Sync IntelliJ Gradle module list with `planner`, `planner-app`, and `frontend`
- Track project code-style preference under `.idea/codeStyles/`

## [0.2.0] - 2026-07-31

### Added

- Register Gradle subprojects `planner`, `planner-app`, and `frontend`
- `ROOT_README.md` as the root module README (local full-stack run docs)
- Root `dev` task and `scripts/dev.ps1` / `scripts/dev.sh` (API `:8080`, then Vite `:5173`)
- Shared Cursor rules: `agent-git-workflow`, `kafka-integration`, `module-readme`, and versioning updates
- Rule that top-level `README.md` ships with product releases only
- `CONTRIBUTING.md` notes on module README scope and tone
- Public `BACKLOG.md`
- Ignore local backend logs under `scripts/`

## [0.1.0] - 2026-06-23

### Added

- Multi-module Gradle root configuration
- Shared Java test setup for subprojects with the `java` plugin
- Centralized product release and root module versions in `gradle.properties`
- Root changelog and product release manifest conventions
- Project rules for versioning, strict module commit boundaries, commit naming/body style, commit integrity checks, and shared/local Cursor rule boundaries
- Secret handling conventions, ignored local secrets, and configuration examples directory
- Development conventions in `CONTRIBUTING.md`
- Placeholder project README
- Apache License 2.0
