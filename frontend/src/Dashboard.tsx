import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState, type ReactNode } from "react";
import { logout } from "./api/auth";
import { isApiError } from "./api/errors";
import { fetchDashboard, updatePolicy } from "./api/planner";
import { ForwardPlanSection } from "./ForwardPlanSection";
import { PlanTodaySection } from "./PlanTodaySection";
import { SessionAccountMenu } from "./SessionAccountMenu";
import { WatchArchiveSection } from "./WatchArchiveSection";
import { clearUserScopedQueries, DASHBOARD_QUERY_KEY } from "./session";
import type { CurrentUser } from "./types";
import { useRefreshDashboardOnDayChange } from "./useRefreshDashboardOnDayChange";

function PageShell({
  children,
  username,
  treatPlanAsWatched,
  onLogout,
  logoutPending,
}: {
  children: ReactNode;
  username: string;
  treatPlanAsWatched?: boolean;
  onLogout: () => void;
  logoutPending: boolean;
}) {
  return (
    <main className="page app-enter">
      <div className="page-top">
        <p className="eyebrow">WatchNest</p>
        <SessionAccountMenu
          username={username}
          treatPlanAsWatched={treatPlanAsWatched}
          onLogout={onLogout}
          logoutPending={logoutPending}
        />
      </div>
      {children}
    </main>
  );
}

export function Dashboard({ user }: { user: CurrentUser }) {
  const queryClient = useQueryClient();
  const dashboardQuery = useQuery({
    queryKey: DASHBOARD_QUERY_KEY,
    queryFn: fetchDashboard,
    retry: false,
  });

  const [weekdayLimit, setWeekdayLimit] = useState(2);
  const [weekendLimit, setWeekendLimit] = useState(4);

  const dashboard = dashboardQuery.data;
  useRefreshDashboardOnDayChange(dashboard?.today);

  useEffect(() => {
    if (!dashboard) {
      return;
    }
    setWeekdayLimit(dashboard.policy.weekdayEpisodeLimit);
    setWeekendLimit(dashboard.policy.weekendEpisodeLimit);
  }, [dashboard]);

  useEffect(() => {
    if (isApiError(dashboardQuery.error) && dashboardQuery.error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  }, [dashboardQuery.error, queryClient]);

  const policyMutation = useMutation({
    mutationFn: updatePolicy,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEY }),
    onError: (error) => {
      if (isApiError(error) && error.status === 401) {
        clearUserScopedQueries(queryClient);
      }
    },
  });

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearUserScopedQueries(queryClient);
    },
  });

  const shell = {
    username: user.username,
    treatPlanAsWatched: dashboardQuery.data?.treatPlanAsWatched,
    onLogout: () => logoutMutation.mutate(),
    logoutPending: logoutMutation.isPending,
  };

  if (isApiError(dashboardQuery.error) && dashboardQuery.error.status === 401) {
    return (
      <PageShell {...shell}>
        <p>Returning to sign in...</p>
      </PageShell>
    );
  }

  if (dashboardQuery.isPending) {
    return (
      <PageShell {...shell}>
        <p>Loading library...</p>
      </PageShell>
    );
  }

  if (dashboardQuery.isError || !dashboard) {
    return (
      <PageShell {...shell}>
        <p>Failed to load library dashboard.</p>
      </PageShell>
    );
  }

  const submitPolicy = (event: FormEvent) => {
    event.preventDefault();
    policyMutation.mutate({
      weekdayEpisodeLimit: weekdayLimit,
      weekendEpisodeLimit: weekendLimit,
    });
  };

  return (
    <PageShell {...shell}>
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
            <span className="label">Planned</span>
            <strong className="value">{dashboard.status.episodesPlanned}</strong>
          </div>
          <div>
            <span className="label">Remaining</span>
            <strong className="value">{dashboard.status.episodesRemaining}</strong>
          </div>
        </div>
        {dashboard.status.overQuota ? (
          <p className="status-note">
            Daily limit exceeded. Extra plan lines stay on today; they are not archive yet.
          </p>
        ) : dashboard.status.canAddAnotherEpisode ? (
          <p className="status-note ok">Another episode fits today&apos;s limit.</p>
        ) : (
          <p className="status-note ok">Today&apos;s limit is reached.</p>
        )}
      </section>

      <section className="grid">
        <PlanTodaySection
          planToday={dashboard.planToday}
          treatPlanAsWatched={dashboard.treatPlanAsWatched}
        />

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

      <ForwardPlanSection today={dashboard.today} />
      <WatchArchiveSection today={dashboard.today} />
    </PageShell>
  );
}
