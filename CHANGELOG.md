# Root Changelog

Changelog for the non-detachable `root` module.

The current `root` module version is stored in `gradle.properties` as `rootVersion`.
Global product releases are tracked separately in `RELEASES.md`.

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
