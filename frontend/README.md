# frontend

React + Vite + TypeScript SPA: UI for the personal watch library.

**Version:** `version` in `package.json` (see `CHANGELOG.md`).

## Purpose

- splash with calendar context;
- dashboard for today’s quota, watch log, and policy;
- HTTP client for `/api/v1`;
- dark theme only.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Splash | `SplashScreen`, `splashDate`: week/month/year context, skip-to-app |
| Dashboard | `App`: quota card, watch log form, policy form |
| API client | `api/planner`: `fetchDashboard`, `logWatchEvent`, `updatePolicy` |
| Day change | `useRefreshDashboardOnDayChange`: invalidate dashboard after local midnight |
| Types | `types.ts`: dashboard/policy/watch DTOs aligned with API JSON |
| Build | `build.gradle.kts`: Gradle tasks that delegate to npm/Vite |

## Constraints

- Quota rules are not computed in the UI; responses from `/api/v1` are displayed as received.
- Dev server proxies `/api` to `http://localhost:8080` (`vite.config.ts`).
- Dark theme only.
- Unit tests via Vitest + Testing Library (jsdom).

## Layout

```text
frontend/
  src/
    api/
    test/
    App.tsx
    SplashScreen.tsx
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

Dev server: `http://localhost:5173`.

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

## Scope (0.1.0)

In scope: splash, dashboard, watch log form, policy form, day-change refresh, dark theme, Vitest coverage for main UI paths.

Out of scope: light theme, auth UI, multi-profile household UI.
