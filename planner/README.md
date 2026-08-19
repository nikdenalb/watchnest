# planner

Java library: watch-library model and screen-time quota analysis.

**Version:** `plannerVersion` in `gradle.properties` (see `CHANGELOG.md`).

## Purpose

- owner profile and screen-time policy;
- dated forward-plan items;
- PlanToday working-day lines;
- watch-event archive facts;
- daily quota status from policy + PlanToday line count.

## Responsibilities

| Area | Responsibility |
| --- | --- |
| Profile | `LibraryProfile`: `id`, `displayName`, `ScreenTimePolicy`, `treatPlanAsWatched` |
| Caps | `LibraryLimits.MAX_TITLES_PER_DATE` (50) |
| Policy | `ScreenTimePolicy`: weekday/weekend episode limits; limit for a `LocalDate` |
| Forward plan | `ForwardPlanItem`: `id`, `ownerId`, `plannedFor`, `contentTitle` |
| Plan today | `PlanToday`: `ownerId`, `forDate`, ordered `PlanTodayLine`s |
| Plan line | `PlanTodayLine`: `id`, `contentTitle`, `checked`, `PlanLineSource` (`FORWARD` / `MANUAL`) |
| History | `WatchEvent`: `id`, `ownerId`, `watchedOn`, `contentTitle` |
| Status | `DailyScreenTimeStatus`: limit, planned, remaining; `isOverQuota()`, `canAddAnotherEpisode()` |
| Calculation | `ScreenTimeQuotaCalculator.summarize(profile, date, planLines)` |

Forward-plan and PlanToday titles are trimmed, non-blank, and at most 120 characters. Ids and dates are required.

## Quota algorithm

1. Resolve episode limit from policy and date (Sat/Sun → weekend limit).
2. Count PlanToday lines (checked and unchecked).
3. `episodesRemaining = max(0, limit - planned)`.
4. Over-quota when `planned > limit` (extra lines remain valid inputs).
5. `canAddAnotherEpisode` when `planned < limit`.

Callers supply the line collection; the calculator does not load or store data.
Archive `WatchEvent`s are not quota inputs.

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

## Scope (0.3.0)

In scope: personal profile, `treatPlanAsWatched` (starting value `false`), shared
title cap `LibraryLimits.MAX_TITLES_PER_DATE`, weekday/weekend episode limits,
dated forward-plan items, PlanToday lines, watch events, daily quota from
PlanToday line count.

Out of scope: content catalog, ratings, auth, multi-profile household model,
persistence.
