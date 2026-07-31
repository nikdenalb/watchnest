import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState, type ReactNode } from "react";
import { fetchDashboard, logWatchEvent, updatePolicy } from "./api/planner";
import { SplashScreen } from "./SplashScreen";
import { useRefreshDashboardOnDayChange } from "./useRefreshDashboardOnDayChange";

function PageShell({ children }: { children: ReactNode }) {
  return (
    <main className="page app-enter">
      <p className="eyebrow">WatchNest</p>
      {children}
    </main>
  );
}

export function App() {
  const queryClient = useQueryClient();
  const dashboardQuery = useQuery({
    queryKey: ["dashboard"],
    queryFn: fetchDashboard,
  });

  const [watchTitle, setWatchTitle] = useState("");
  const [weekdayLimit, setWeekdayLimit] = useState(2);
  const [weekendLimit, setWeekendLimit] = useState(4);
  const [splashDismissed, setSplashDismissed] = useState(false);

  const dashboard = dashboardQuery.data;
  useRefreshDashboardOnDayChange(dashboard?.today);

  useEffect(() => {
    if (!dashboard) {
      return;
    }
    setWeekdayLimit(dashboard.policy.weekdayEpisodeLimit);
    setWeekendLimit(dashboard.policy.weekendEpisodeLimit);
  }, [dashboard]);

  const watchMutation = useMutation({
    mutationFn: logWatchEvent,
    onSuccess: () => {
      setWatchTitle("");
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  const policyMutation = useMutation({
    mutationFn: updatePolicy,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
  });

  const showSplash = !splashDismissed;
  const appReady = dashboardQuery.isFetched;

  if (showSplash) {
    return (
      <SplashScreen appReady={appReady} onDismiss={() => setSplashDismissed(true)} />
    );
  }

  if (dashboardQuery.isError || !dashboard) {
    return (
      <PageShell>
        <p>Failed to load library dashboard.</p>
      </PageShell>
    );
  }

  const submitWatch = (event: FormEvent) => {
    event.preventDefault();
    if (!watchTitle.trim()) {
      return;
    }
    watchMutation.mutate(watchTitle.trim());
  };

  const submitPolicy = (event: FormEvent) => {
    event.preventDefault();
    policyMutation.mutate({
      weekdayEpisodeLimit: weekdayLimit,
      weekendEpisodeLimit: weekendLimit,
    });
  };

  return (
    <PageShell>
      <header className="hero">
        <h1>Your watch day</h1>
        <p className="subtitle">Today: {dashboard.today}</p>
      </header>

      <section className="card quota-card">
        <h2>Screen time today</h2>
        <div className="quota-grid">
          <div>
            <span className="label">Limit</span>
            <strong className="value">{dashboard.status.episodeLimit}</strong>
          </div>
          <div>
            <span className="label">Watched</span>
            <strong className="value">{dashboard.status.episodesWatched}</strong>
          </div>
          <div>
            <span className="label">Remaining</span>
            <strong className="value">{dashboard.status.episodesRemaining}</strong>
          </div>
        </div>
        {dashboard.status.overQuota ? (
          <p className="status-note">Daily limit exceeded. Extra watches stay in the log.</p>
        ) : dashboard.status.canWatchAnotherEpisode ? (
          <p className="status-note ok">Another episode fits today&apos;s limit.</p>
        ) : (
          <p className="status-note ok">Today&apos;s limit is reached.</p>
        )}
      </section>

      <section className="grid">
        <article className="card">
          <h2>Log a watch</h2>
          <p className="hint">Use this for TV, DVD, another app, or manual entry.</p>
          <form onSubmit={submitWatch}>
            <label htmlFor="contentTitle">What was watched?</label>
            <input
              id="contentTitle"
              value={watchTitle}
              onChange={(event) => setWatchTitle(event.target.value)}
              placeholder="Episode title"
              maxLength={120}
              required
            />
            <button type="submit" disabled={watchMutation.isPending}>
              Add to watch log
            </button>
          </form>
        </article>

        <article className="card">
          <h2>Screen-time rules</h2>
          <p className="hint">Weekday and weekend episode limits for your library.</p>
          <form onSubmit={submitPolicy}>
            <label htmlFor="weekdayEpisodeLimit">Weekday limit</label>
            <input
              id="weekdayEpisodeLimit"
              type="number"
              min={0}
              max={20}
              value={weekdayLimit}
              onChange={(event) => setWeekdayLimit(Number(event.target.value))}
              required
            />
            <label htmlFor="weekendEpisodeLimit">Weekend limit</label>
            <input
              id="weekendEpisodeLimit"
              type="number"
              min={0}
              max={20}
              value={weekendLimit}
              onChange={(event) => setWeekendLimit(Number(event.target.value))}
              required
            />
            <button type="submit" disabled={policyMutation.isPending}>
              Save rules
            </button>
          </form>
        </article>
      </section>

      <section className="card">
        <h2>Today&apos;s watch log</h2>
        {dashboard.todayEvents.length === 0 ? (
          <p className="hint">No watches logged yet today.</p>
        ) : (
          <ul className="event-list">
            {dashboard.todayEvents.map((event) => (
              <li key={event.id}>{event.contentTitle}</li>
            ))}
          </ul>
        )}
      </section>
    </PageShell>
  );
}
