# WatchNest Backlog

Product ideas and deferred work. Not a commitment or schedule.

## Family (next step after personal MVP)

- **Household / family profiles** — Add multiple profiles under one household (kids and adults). Keep `ownerId` on watch events so history stays keyed to a profile when family arrives.

## Calendar & days

- **Holiday / free-day display** — Show non-working days from the user's country labor calendar (not only Sat/Sun). Prefer a domain `WorkCalendar` / day-kind resolver in `planner`, country on the personal profile, and API-marked days for the UI. Frontend should render flags, not own holiday logic.
- **User special days** — Let the user mark personal special days (birthdays, custom free days) and show them on week/month/year calendar surfaces alongside holidays. Family events can wait until household profiles exist.
