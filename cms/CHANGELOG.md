# CMS Changelog

Changelog for the `cms` module.

## [0.2.0] - 2026-08-28

### Added

- `403` `demo_account` alert with the exact server copy; Create, Save, and Delete stay visible
- Confirmed delete that is blocked closes the dialog so the alert is visible on the selected title

### Changed

- Unsafe-request retry remains one-shot and only for `csrf_invalid`, never `demo_account`

## [0.1.0] - 2026-08-25

### Added

- React + Vite + TypeScript SPA for the owned title catalog
- Sign-in against `/cms/api/v1` with a dedicated CMS CSRF cache
- Title search, list, create, edit, and confirmed hard delete
- Module README and Gradle npm tasks
