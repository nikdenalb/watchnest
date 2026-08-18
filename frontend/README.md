# frontend

React + Vite + TypeScript SPA: UI for the personal watch library.

**Version:** `version` in `package.json` (see `CHANGELOG.md`).

## Purpose

- splash with calendar context;
- username/password auth (session cookie + CSRF);
- dashboard for today’s quota, PlanToday, dated forward plan, and policy;
- monthly watch history on the same page;
- HTTP client for `/api/v1`;
- dark theme only.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Splash | `SplashScreen`, `splashDate`: week/month/year context; ready when auth state is known |
| Auth | `AuthScreen` + `api/auth`: register, login, logout, `/me` |
| Dashboard | `Dashboard`: quota card, PlanToday, policy form, session bar |
| Plan today | `PlanTodaySection`: list, checkbox, add, remove |
| Forward plan | `ForwardPlanSection`: one dated plan; Week / Month / Year display ranges |
| Watch history | `WatchArchiveSection`: month diary; gear → overlay dialogs for past-day correction |
| Overlay | `OverlayDialog`, `ArchiveDayDialog`: `role="dialog"` (not native `<dialog>`) |
| App shell | `App`: splash → auth or dashboard based on `/me` |
| HTTP | `api/http`: `credentials: "include"`, CSRF header on unsafe methods, one stale-CSRF retry |
| Planner API | `api/planner`: dashboard, PlanToday, forward plan, archive GET/POST/PATCH/DELETE, policy |
| Session cache | `session`: clear `me`, `dashboard`, `plan-forward`, and `watch-events` on logout / `401` |
| Day change | `useRefreshDashboardOnDayChange`: invalidate dashboard and forward-plan keys after local midnight |
| Types | `types.ts`: auth + dashboard/plan/watch DTOs aligned with API JSON |
| Build | `build.gradle.kts`: Gradle tasks that delegate to npm/Vite |

## Auth and CSRF

- Startup: `GET /api/v1/auth/me` with cookies. `401` → logged out (`null`).
- Dashboard loads only after an authenticated session is known.
- Unsafe methods (`POST`/`PUT`/`PATCH`/`DELETE`) send the CSRF header from `GET /api/v1/auth/csrf`.
- CSRF is refreshed after register, login, and logout (best-effort; auth success does not fail if refresh fails).
- One automatic retry on `403` / `csrf_invalid`.
- Logout and planner `401` clear user-scoped Query cache (`me`, `dashboard`, `plan-forward`, `watch-events`), then show the auth screen.
- Vite proxies `/api` → `http://localhost:8080`. Credentialed calls need CORS for `http://localhost:5173`.

## Plan today and forward plan

- PlanToday: `POST`/`PATCH`/`DELETE` `/api/v1/plan/today/lines`. Quota counts lines (checked and unchecked). Add stays enabled when remaining is 0.
- Forward plan: `GET /api/v1/plan/forward?from&to`, `POST /api/v1/plan/forward`, `DELETE /api/v1/plan/forward/{id}`. Query keys: `["plan-forward"]` and `["plan-forward", from, to]`.
- Week / Month / Year are display ranges over the same dated collection (ISO week Mon–Sun; full calendar month/year). The list is grouped by date; there is no calendar grid.
- Forward add `min` and default are tomorrow (`addDays(today, 1)`). This form never POSTs PlanToday. Leftover items with `plannedFor <= today` are read-only (no Remove).
- Every plan mutation invalidates dashboard and forward-plan queries.
- Day-change refresh invalidates dashboard and forward-plan keys so the next GET can roll on the server.

## Watch history

- `GET /api/v1/watch-events?from&to` via `fetchWatchEvents`. Query keys: `["watch-events"]` and `["watch-events", from, to]`.
- Month bounds come from `dashboard.today` (`archiveMonthRange`). Current month is clipped to `today`; past months use the full calendar month.
- The public list is a diary (day heading + titles). Correction is behind gears: day group (`watchedOn < today`) and header “Correct a day”.
- Day dialog owns `GET /watch-events?from={date}&to={date}`. Add/rename/delete call POST/PATCH/DELETE. Success invalidates `watch-events` and `dashboard`, not forward plan.
- Archive loading and non-401 errors stay in the Watch history card. Quota, PlanToday, and policy stay usable.
- Same-day PlanToday titles are not archive rows. Checked titles become watch events when the day rolls on the server. Today-dated leftover archive rows are visible without a gear.
- Day-change refresh does not invalidate archive directly; a new `today` changes the current-month `to` and therefore the archive key.

## Constraints

- Quota rules are not computed in the UI; `/api/v1` responses are shown as received.
- Never send `ownerId` from the browser.
- Archive diary is display-only. Past-day add/rename/delete happen in overlay dialogs (`watchedOn` before `dashboard.today`).
- Splash is decorative calendar chrome, not the plan editor.
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
    Dashboard.tsx
    PlanTodaySection.tsx
    ForwardPlanSection.tsx
    WatchArchiveSection.tsx
    ArchiveDayDialog.tsx
    OverlayDialog.tsx
    SplashScreen.tsx
    archiveMonthRange.ts
    forwardPlanRange.ts
    session.ts
    splashDate.ts
    useRefreshDashboardOnDayChange.ts
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

## Scope (0.5.0)

In scope: splash, session auth UI, CSRF client, dashboard, PlanToday list (checkbox, add, remove), dated forward plan with Week / Month / Year display ranges (add from tomorrow), policy form, monthly watch history diary with past-day correction dialogs, day-change refresh, dark theme, Vitest coverage for auth, plan, dashboard, and archive paths.

Out of scope: calendar grid, holidays, catch-up for missed days, splash as plan editor, date moves, native `<dialog>`, OAuth, email, password reset.
