# WatchNest Backlog

Product ideas and deferred work. Not a commitment or schedule.

## Deploy / TLS

- **Custom domain (e.g. `watchnest.ru`)** — Register a `.ru` (or other) domain,
  point DNS at the demo VM or ALB, and use that hostname instead of a raw
  public IP for the public site.
- **HTTPS on the demo host** — After a domain exists: TLS via on-VM certbot or
  (preferred later) Yandex Certificate Manager + ALB.
- **Yandex Certificate Manager + ALB TLS** — Move HTTPS termination from
  on-VM nginx + certbot (Let’s Encrypt) to Yandex Cloud Application Load
  Balancer and Certificate Manager. Keep Compose `app` / `db` / nginx behind
  the balancer; drop on-box Let’s Encrypt from the required path when ALB is
  live.

## Auth (separate module / service)

- **Standalone `auth` module or service** — Not needed while login lives in
  `planner-app` (HTTP session + Spring Security) on top of the `identity`
  domain library. Extract when more than one deployable client/service must
  share the same sign-in (for example cloud API + PC app + Android against one
  account store), or when identity/auth must ship and scale independently of
  planner. Until then keep auth transport in `planner-app` and account rules in
  `identity`.

## Analysis / recommendations

- **Analysis agent / recommendations engine** — Use watch history, reviews,
  ratings, and stated wishes to suggest titles and plans for one person.

## Calendar & days

- **Holiday / free-day display** — Show non-working days from the user's country labor calendar (not only Sat/Sun). Prefer a domain `WorkCalendar` / day-kind resolver in `planner`, country on the personal profile, and API-marked days for the UI. Frontend should render flags, not own holiday logic.
- **User special days** — Let the user mark personal special days (birthdays, custom free days) and show them on week/month/year calendar surfaces alongside holidays.

## Follow-up, not urgent

- **Day-change handling** — Design when a calendar day closes: timezone,
  days the user never opened, and when checked titles flush to the archive.
  The first cut only rolls on the next authenticated request after server
  `today` changes; that heuristic is temporary.
- **Leftover titles at day roll** — Decide what happens to unchecked items
  on yesterday’s plan: reschedule into the dated forward plan, carry into the
  next day, or stay discarded. The first cut discards them.
- **Catch-up for missed working days** — Design how a user can still work
  through a plan for days they skipped or left unchecked, when they did
  watch those titles and simply did not mark them. The first cut does not
  keep skipped-day documents and does not offer a catch-up flow: the next
  visit flushes only the stale PlanToday (checked titles to the archive on
  that `forDate`, unchecked titles discarded), then opens today.

## Late plan

- **Household and collaborative viewing** — Family/household libraries, shared
  sessions, and recommendations that balance several people’s tastes and
  constraints. Not in the near product line; keep history keyed so a later
  profile/`ownerId` split stays possible.

