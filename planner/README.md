# planner

Java library: watch-library model and screen-time quota analysis.

**Version:** `plannerVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- owner profile and screen-time policy;
- watch events;
- daily quota status derived from policy + events.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Profile | `LibraryProfile`: `id`, `displayName`, `ScreenTimePolicy` |
| Policy | `ScreenTimePolicy`: weekday/weekend episode limits; limit for a `LocalDate` |
| History | `WatchEvent`: `id`, `ownerId`, `watchedOn`, `contentTitle` |
| Status | `DailyScreenTimeStatus`: limit, watched, remaining; `isOverQuota()`, `canWatchAnotherEpisode()` |
| Calculation | `ScreenTimeQuotaCalculator.summarize(profile, date, events)` |

## Quota algorithm

1. Resolve episode limit from policy and date (Sat/Sun → weekend limit).
2. Count events with matching `ownerId` and `watchedOn`.
3. `episodesRemaining = max(0, limit - watched)`.
4. Over-quota when `watched > limit` (events beyond limit remain valid inputs).

Callers supply the event collection; the calculator does not load or store data.

## Constraints

- JDK only (no framework dependencies).
- Domain records, validation, and quota calculation without I/O.
- JVM unit tests (no container or network).

## Layout

```text
planner/
  src/main/java/dev/watchnest/planner/
    domain/
    policy/
  src/test/java/...
  build.gradle.kts
  gradle.properties
  CHANGELOG.md
  README.md
```

## Build and test

```bash
./gradlew :planner:test
./gradlew :planner:build
```

Windows:

```powershell
.\gradlew.bat :planner:test
```

## Scope (0.1.0)

In scope: personal profile, weekday/weekend episode limits, watch events, daily
quota summary.

Out of scope: week schedule, content catalog, ratings, auth, multi-profile
household model, persistence.
