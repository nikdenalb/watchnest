# Planner Changelog

Changelog for the `planner` module.

## [0.2.0] - 2026-08-17

### Added

- Dated forward-plan items (`ForwardPlanItem` with `plannedFor`)
- PlanToday working-day lines (`PlanToday` / `PlanTodayLine`, source `FORWARD` or `MANUAL`)
- Title rules for plan types: trim, non-blank, max 120 characters

### Changed

- Daily quota counts PlanToday lines (checked and unchecked), not watch events
- Status fields: `episodesPlanned` and `canAddAnotherEpisode` (`episodesRemaining` unchanged), so names match plan-line quota instead of archive watches

## [0.1.0] - 2026-07-31

### Added

- Personal library profile with weekday and weekend screen-time policy
- Watch event model keyed by owner for daily episode counting
- Daily screen-time summary with watched, remaining, and over-quota state
- Screen-time quota calculator derived from policy and watch events
- Module README
