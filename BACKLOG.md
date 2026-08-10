# WatchNest Backlog

Product ideas and deferred work. Not a commitment or schedule.

## Deploy / TLS (high priority)

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

## Family (next step after personal MVP)

- **Household / family profiles** — Add multiple profiles under one household (kids and adults). Keep `ownerId` on watch events so history stays keyed to a profile when family arrives.

## Calendar & days

- **Holiday / free-day display** — Show non-working days from the user's country labor calendar (not only Sat/Sun). Prefer a domain `WorkCalendar` / day-kind resolver in `planner`, country on the personal profile, and API-marked days for the UI. Frontend should render flags, not own holiday logic.
- **User special days** — Let the user mark personal special days (birthdays, custom free days) and show them on week/month/year calendar surfaces alongside holidays. Family events can wait until household profiles exist.
