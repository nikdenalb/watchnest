# frontend

React + Vite + TypeScript SPA: UI for the personal watch diary.

**Version:** `version` in `package.json` (see `CHANGELOG.md`).

## Purpose

- splash with calendar context;
- username/password auth (session cookie + CSRF);
- one monthly editable watch diary after sign-in;
- HTTP client for `/api/v1`;
- dark theme only.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Splash | `SplashScreen`, `splashDate`: week/month/year context; ready when auth state is known |
| Auth | `AuthScreen` + `api/auth`: register, login, logout, `/me` |
| Diary | `WatchDiary` + `WatchArchiveSection`: month list; gear → overlay dialogs for any date |
| Account | `SessionAccountMenu`: username disclosure + Log out |
| Overlay | `OverlayDialog`, `ArchiveDayDialog`: `role="dialog"` (not native `<dialog>`) |
| App shell | `App`: splash → auth or diary based on `/me` |
| HTTP | `api/http`: `credentials: "include"`, `cache: "no-store"`, CSRF header on unsafe methods, one stale-CSRF retry |
| Watch-event API | `api/planner`: GET/POST/PATCH/DELETE `/api/v1/watch-events` |
| Session cache | `session`: `me` and `watch-events`; logout / `401` clear both |
| Types | `types.ts`: auth + watch-event DTOs aligned with API JSON |
| Build | `build.gradle.kts`: Gradle tasks that delegate to npm/Vite |

## Auth and CSRF

- Startup: `GET /api/v1/auth/me` with cookies. `401` → logged out (`null`).
- After `/me` resolves to a user, the diary renders.
- Unsafe methods (`POST`/`PUT`/`PATCH`/`DELETE`) send the CSRF header from `GET /api/v1/auth/csrf`.
- CSRF fetches use `cache: "no-store"` so logout cannot reuse a cached token after the cookie is cleared.
- CSRF is refreshed after register, login, and logout (best-effort; auth success does not fail if refresh fails).
- One automatic retry on `403` / `csrf_invalid`.
- Logout and watch-event `401` clear user-scoped Query cache (`me`, `watch-events`), then show the auth screen.
- Vite proxies `/api` → `http://localhost:8080`. Credentialed calls need CORS for `http://localhost:5173`.

## Session menu

- Logged-in header shows only the username (`user.username`, not title-cased).
- Click the username to open a custom dropdown (not `OverlayDialog`, not native `<dialog>`). The panel is labelled from the button (`aria-expanded`, `aria-controls`). It is not `role="menu"`.
- The panel holds **Log out**. Close on Escape, pointer down outside the bar and panel, or a second username click. No focus trap.

## Watch diary

- Home is the client-local current calendar month. Client-local today is a navigation and picker default only; it does not restrict writes.
- `GET /api/v1/watch-events?from&to` via `fetchWatchEvents`. Query keys: `["watch-events"]` and `["watch-events", from, to]`.
- The selected month is queried in full, including dates after today. Previous and next month stay enabled with no future ceiling.
- The public list is a diary (day heading + titles). Every listed day has a gear, including today and future days.
- Header gear **Edit a day** opens a date picker with no `max`. Default is client-local today. Any non-empty date continues into the day dialog, including empty days.
- Day dialog owns `GET /watch-events?from={date}&to={date}`. Add/rename/delete call POST/PATCH/DELETE. Success invalidates only `watch-events`.
- Diary loading and non-401 errors stay in the diary card. Cap and validation errors stay in the day dialog.
- PATCH changes only `contentTitle`; it cannot move an event to another date.

## Constraints

- Never send `ownerId` from the browser.
- Splash is decorative calendar chrome, not the diary editor.
- Dark theme only.
- Unit tests via Vitest + Testing Library (jsdom).
- Node **24** LTS (`>=24 <25`) is required for `npm` and Gradle frontend tasks (`.nvmrc`).

## Layout

```text
frontend/
  src/
    api/
    test/
    App.tsx
    AuthScreen.tsx
    WatchDiary.tsx
    SessionAccountMenu.tsx
    WatchArchiveSection.tsx
    ArchiveDayDialog.tsx
    OverlayDialog.tsx
    SplashScreen.tsx
    archiveMonthRange.ts
    session.ts
    splashDate.ts
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

Node **24** LTS is required (`engines.node` `>=24 <25`, `.nvmrc`). Same constraint applies to `./gradlew :frontend:npm*` tasks.

```bash
./gradlew :frontend:npmTest
./gradlew :frontend:npmBuild
./gradlew :frontend:npmDev
```

Windows:

```powershell
.\gradlew.bat :frontend:npmDev
```

Or:

```bash
cd frontend
npm install
npm run dev
```

Dev server: `http://localhost:5173`. API must be reachable at the Vite proxy target.

## Tasks

| Command | Purpose |
| --- | --- |
| `./gradlew :frontend:npmInstall` | `npm install` |
| `./gradlew :frontend:npmTest` / `:frontend:check` | Vitest (+ build on check) |
| `./gradlew :frontend:npmBuild` / `:frontend:assemble` | production build |
| `./gradlew :frontend:npmDev` | Vite dev server |
| `npm run test` | Vitest |
| `npm run build` | Typecheck + production build |
| `npm run preview` | Preview production build |

## Scope (0.8.0)

In scope: splash, session auth UI, CSRF client, username/logout disclosure, one monthly editable watch diary (any date, full-month query, unrestricted future navigation), watch-event HTTP client, dark theme, Vitest coverage for auth and diary paths.

Out of scope: calendar grid, holidays, splash as diary editor, date moves on PATCH, native `<dialog>`, OAuth, email, password reset.
