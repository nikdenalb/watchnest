# Root Changelog

Changelog for the non-detachable `root` module.

Product releases are listed in `RELEASES.md`.

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
