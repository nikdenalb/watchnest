import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { isApiError } from "./api/errors";
import { fetchWatchEvents } from "./api/planner";
import {
  addCalendarMonths,
  archiveMonthRange,
  formatDayHeading,
  formatMonthLabel,
  isNextMonthDisabled,
  yearMonthFromIso,
} from "./archiveMonthRange";
import { clearUserScopedQueries, watchEventsQueryKey } from "./session";
import type { WatchEvent } from "./types";

export function groupWatchEventsByDay(events: WatchEvent[]): { watchedOn: string; events: WatchEvent[] }[] {
  const groups: { watchedOn: string; events: WatchEvent[] }[] = [];
  for (const event of events) {
    const last = groups.at(-1);
    if (last && last.watchedOn === event.watchedOn) {
      last.events.push(event);
    } else {
      groups.push({ watchedOn: event.watchedOn, events: [event] });
    }
  }
  return groups;
}

export function WatchArchiveSection({ today }: { today: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState(() => yearMonthFromIso(today));
  const { from, to } = archiveMonthRange(today, selected);
  const previous = addCalendarMonths(selected, -1);
  const next = addCalendarMonths(selected, 1);
  const nextDisabled = isNextMonthDisabled(today, selected);

  const archiveQuery = useQuery({
    queryKey: watchEventsQueryKey(from, to),
    queryFn: () => fetchWatchEvents(from, to),
    retry: false,
  });

  useEffect(() => {
    if (isApiError(archiveQuery.error) && archiveQuery.error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  }, [archiveQuery.error, queryClient]);

  const unauthorized = isApiError(archiveQuery.error) && archiveQuery.error.status === 401;

  return (
    <section className="card archive-card">
      <h2>Watch history</h2>
      <div className="archive-month-nav">
        <button
          type="button"
          className="linkish"
          aria-label="Previous month"
          onClick={() => setSelected(previous)}
        >
          ← {formatMonthLabel(previous)}
        </button>
        <span className="archive-month-label">{formatMonthLabel(selected)}</span>
        <button
          type="button"
          className="linkish"
          aria-label="Next month"
          disabled={nextDisabled}
          onClick={() => setSelected(next)}
        >
          {formatMonthLabel(next)} →
        </button>
      </div>

      {unauthorized ? (
        <p className="hint">Returning to sign in...</p>
      ) : archiveQuery.isPending ? (
        <p className="hint">Loading watch history...</p>
      ) : archiveQuery.isError ? (
        <>
          <p className="status-note">Could not load watch history.</p>
          <button type="button" className="linkish" onClick={() => void archiveQuery.refetch()}>
            Retry
          </button>
        </>
      ) : !archiveQuery.data || archiveQuery.data.events.length === 0 ? (
        <p className="hint">No watches logged this month.</p>
      ) : (
        groupWatchEventsByDay(archiveQuery.data.events).map((group) => (
          <div key={group.watchedOn} className="archive-day">
            <h3>{formatDayHeading(group.watchedOn)}</h3>
            <ul className="event-list">
              {group.events.map((event) => (
                <li key={event.id}>{event.contentTitle}</li>
              ))}
            </ul>
          </div>
        ))
      )}
    </section>
  );
}
