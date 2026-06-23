# WatchNest

WatchNest is an early-stage work-in-progress project.

The project is expected to grow as a multi-module system. Module boundaries and
product details are intentionally not finalized yet.

## Status

- Early project structure setup
- Modules are introduced only when their boundaries become clear

## Versioning

- Product releases use SemVer and are tracked in `RELEASES.md`.
- Each module keeps its own SemVer version and changelog.
- The root module is non-detachable and keeps its version in `gradle.properties`.

## Development

Development conventions are documented in `CONTRIBUTING.md`.

Configuration examples live in `config/examples/`. Local secrets are kept out of
git and may be stored under `secrets/`.

## License

This project is licensed under the Apache License 2.0.
