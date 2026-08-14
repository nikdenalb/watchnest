# frontend

React + Vite + TypeScript SPA: UI for the personal watch library.

**Version:** `version` in `package.json` (see `CHANGELOG.md`).

## Purpose

- splash with calendar context;
- username/password auth (session cookie + CSRF);
- dashboard for today’s quota, watch log, and policy;
- monthly watch history on the same page;
- HTTP client for `/api/v1`;
- dark theme only.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Splash | `SplashScreen`, `splashDate`: week/month/year context; ready when auth state is known |
| Auth | `AuthScreen` + `api/auth`: register, login, logout, `/me` |
| Dashboard | `Dashboard`: quota card, watch log form, policy form, session bar |
| Watch history | `WatchArchiveSection`: month diary list; `archiveMonthRange` from `dashboard.today` |
| App shell | `App`: splash → auth or dashboard based on `/me` |
| HTTP | `api/http`: `credentials: "include"`, CSRF header on unsafe methods, one stale-CSRF retry |
| Planner API | `api/planner`: `fetchDashboard`, `fetchWatchEvents`, `logWatchEvent`, `updatePolicy` |
| Session cache | `session`: clear `me`, `dashboard`, and `watch-events` Query keys on logout / `401` |
| Day change | `useRefreshDashboardOnDayChange`: invalidate dashboard after local midnight |
| Types | `types.ts`: auth + dashboard/policy/watch DTOs aligned with API JSON |
| Build | `build.gradle.kts`: Gradle tasks that delegate to npm/Vite |

## Auth and CSRF

- Startup: `GET /api/v1/auth/me` with cookies. `401` → logged out (`null`).
- Dashboard loads only after an authenticated session is known.
- Unsafe methods (`POST`/`PUT`) send the CSRF header from `GET /api/v1/auth/csrf`.
- CSRF is refreshed after register, login, and logout (best-effort; auth success does not fail if refresh fails).
- One automatic retry on `403` / `csrf_invalid`.
- Logout and planner `401` clear user-scoped Query cache (`me`, `dashboard`, `watch-events`), then show the auth screen.
- Vite proxies `/api` → `http://localhost:8080`. Credentialed calls need CORS for `http://localhost:5173`.

## Watch history

- `GET /api/v1/watch-events?from&to` via `fetchWatchEvents`. Query keys: `["watch-events"]` and `["watch-events", from, to]`.
- Month bounds come from `dashboard.today` (`archiveMonthRange`). Current month is clipped to `today`; past months use the full calendar month.
- Archive loading and non-401 errors stay in the Watch history card. Quota, log, and policy stay usable.
- After `logWatchEvent`, dashboard and `watch-events` queries are invalidated.
- Day-change refresh invalidates dashboard only; a new `today` changes the current-month `to` and therefore the archive key.

## Constraints

- Quota rules are not computed in the UI; `/api/v1` responses are shown as received.
- Never send `ownerId` from the browser.
- Archive is display-only; `POST /watch-events` still stamps today on the server.
- Dark theme only.
- Unit tests via Vitest + Testing Library (jsdom).

## Layout

```text
frontend/
  src/
    api/
    test/
    App.tsx
    AuthScreen.tsx
    Dashboard.tsx
    WatchArchiveSection.tsx
    SplashScreen.tsx
    archiveMonthRange.ts
    session.ts
    splashDate.ts
    useRefreshDashboardOnDayChange.ts
    main.tsx
    index.css
    types.ts
  index.html
  package.json
  vite.config.ts
  build.gradle.kts
  CHANGELOG.md
  README.md
```

## Build and run

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

## Scope (0.3.0)

In scope: splash, session auth UI, CSRF client, dashboard, watch log form, policy form, monthly watch history list, day-change refresh, dark theme, Vitest coverage for auth, dashboard, and archive paths.

Out of scope: week/month/year plan, calendar grid, logging a watch on a past date, OAuth, email, password reset, light theme.
