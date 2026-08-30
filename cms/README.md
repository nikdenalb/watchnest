# cms

React + Vite + TypeScript SPA: editor for the owned title catalog.

**Version:** `version` in `package.json` (see `CHANGELOG.md`).

## Purpose

- sign-in for provisioned CMS accounts;
- list and search titles by English name;
- create, edit, and hard-delete catalog titles;
- HTTP client for `/cms/api/v1`;
- dark theme only.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Auth | `SignInScreen` + `api/auth`: login, logout, `/me` (no registration) |
| Catalog | `CatalogEditor`: search, list, create form, selected-title edit, confirmed delete |
| HTTP | `api/http`: `credentials: "include"`, `cache: "no-store"`, GET CSRF immediately before each unsafe method, one stale-CSRF retry |
| Title API | `api/titles`: list/search, get, create, update, delete |
| Session cache | `session`: clear `cms-me` and `cms-titles` on logout / `401`; `App` invalidates `cms-me` on bfcache restore |
| Types | `types.ts`: CMS user + title DTOs aligned with API JSON |
| Build | `build.gradle.kts`: Gradle tasks that delegate to npm/Vite |

## Auth and CSRF

- Startup: `GET /cms/api/v1/me` with cookies. `401` → signed out (`null`).
- Catalog UI loads only after an authenticated CMS session is known.
- Unsafe methods (`POST`/`PUT`/`PATCH`/`DELETE`) always `GET /cms/api/v1/csrf` immediately before the call (`credentials: "include"`, `cache: "no-store"`) and send that response’s `headerName` and `token`. A token from an earlier request is not reused.
- CSRF fetches use `cache: "no-store"` so logout cannot reuse a cached token after the CSRF cookie is cleared.
- Header name comes from that response (`X-WATCHNEST-CMS-XSRF-TOKEN`).
- CSRF is refreshed after login and logout (best-effort; auth success does not fail if refresh fails).
- One automatic retry on `403` `csrf_invalid` only. `403` `demo_account` is not retried.
- Logout and title `401` clear CMS Query cache (`cms-me`, `cms-titles`), then show sign-in.
- A back-forward cache restore (`pageshow` with `persisted`) invalidates `cms-me` so a revoked session cannot keep the catalog on screen.
- This client never calls `/api/v1/auth/*`.
- Vite proxies `/cms/api` → `http://localhost:8080` with the path unchanged.

## Catalog editor

- Empty search loads all titles (`GET /cms/api/v1/titles`).
- Non-empty search submits `GET /cms/api/v1/titles?q=`.
- Create and edit send type, English name, original name, year, description, genres, and countries. Empty optionals are JSON `null`.
- Type is a select of `FILM`, `TV_SERIES`, `MINI_SERIES`, `TV_SHOW`.
- Edit is a full replacement (`PUT`). Delete is `DELETE` after an explicit confirmation dialog.
- `409` `title_already_exists` renders the returned `existingTitle`.
- `403` `demo_account` renders “This is a demonstration account. The change was not applied.” Create, Save, and Delete stay visible. A rejected confirmed delete closes the dialog and keeps the selected title.
- Successful create/update/delete invalidates title queries. Failed writes do not.

## Constraints

- Browser origin is `/cms/`. All API calls use `/cms/api/v1/` only.
- Sign-in only. There is no registration or CMS-account management UI.
- Quota, uniqueness, and canonicalization are not computed in the UI; responses are shown as received.
- Dark theme only.
- Unit tests via Vitest + Testing Library (jsdom).
- Node **24** LTS (`>=24 <25`) is required for `npm` and Gradle CMS tasks (`.nvmrc`).

## Layout

```text
cms/
  src/
    api/
    test/
    App.tsx
    SignInScreen.tsx
    CatalogEditor.tsx
    TitleForm.tsx
    OverlayDialog.tsx
    ConfirmDialog.tsx
    session.ts
    main.tsx
    index.css
    types.ts
  index.html
  package.json
  .nvmrc
  vite.config.ts
  build.gradle.kts
  CHANGELOG.md
  README.md
```

## Build and run

Node **24** LTS is required (`engines.node` `>=24 <25`, `.nvmrc`). Same constraint applies to `./gradlew :cms:npm*` tasks. Gradle tasks require the root build to `include("cms")`.

```bash
./gradlew :cms:npmTest
./gradlew :cms:npmBuild
./gradlew :cms:npmDev
```

Windows:

```powershell
.\gradlew.bat :cms:npmDev
```

Or:

```bash
cd cms
npm install
npm run dev
```

Dev server: `http://localhost:5174/cms/`. API must be reachable at the Vite proxy target.

## Tasks

| Command | Purpose |
| --- | --- |
| `./gradlew :cms:npmInstall` | `npm install` |
| `./gradlew :cms:npmTest` / `:cms:check` | Vitest (+ build on check) |
| `./gradlew :cms:npmBuild` / `:cms:assemble` | production build |
| `./gradlew :cms:npmDev` | Vite dev server on port 5174 |
| `npm run test` | Vitest |
| `npm run build` | Typecheck + production build |
| `npm run preview` | Preview production build |

## Scope (0.2.2)

In scope: sign-in, CMS CSRF client (`cache: "no-store"`; fresh `GET /csrf` immediately before each unsafe request), title search/list, create, full-replace edit, confirmed hard delete, `409` existing-title display, `403` `demo_account` write alert, bfcache restore rechecks `cms-me`, dark theme, Vitest coverage for session and catalog paths.

Out of scope: CMS account registration and password change, episodes/seasons/credits, posters, external catalog ids, genre/country dictionaries.
