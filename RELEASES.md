# Releases

Product release manifest for WatchNest.

Product releases use SemVer: `MAJOR.MINOR.PATCH`.
The current product release version is stored in `gradle.properties` as `productVersion`.
The value `0.0.0` means that the product has not been released yet.

Each detachable module keeps its own version and changelog inside the module directory.
This file records which module versions are included in each product release.

Versioning rules:

- `MAJOR`: incompatible product-level changes.
- `MINOR`: product-level features or notable module additions.
- `PATCH`: product-level fixes and maintenance releases.
