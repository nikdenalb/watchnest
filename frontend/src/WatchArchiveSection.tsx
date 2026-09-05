import { useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState } from "react";
import { isApiError } from "./api/errors";
import { fetchWatchEvents } from "./api/planner";
import {
  addCalendarMonths,
  calendarMonthRange,
  formatDayHeading,
  formatMonthLabel,
  localDateIso,
  yearMonthFromIso,
} from "./archiveMonthRange";
import { ArchiveDayDialog } from "./ArchiveDayDialog";
import { GearButton, OverlayDialog } from "./OverlayDialog";
import { clearUserScopedQueries, watchEventsQueryKey } from "./session";
import type { WatchEvent } from "./types";

const PICKER_TITLE_ID = "diary-picker-dialog-title";

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

export function WatchArchiveSection({ today = localDateIso() }: { today?: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState(() => yearMonthFromIso(today));
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickerDate, setPickerDate] = useState("");
  const [dayDate, setDayDate] = useState<string | null>(null);
  const { from, to } = calendarMonthRange(selected);
  const previous = addCalendarMonths(selected, -1);
  const next = addCalendarMonths(selected, 1);

  const closeDialogs = () => {
    setPickerOpen(false);
    setDayDate(null);
    setPickerDate("");
  };

  const goMonth = (month: typeof previous) => {
    closeDialogs();
    setSelected(month);
  };

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
  const continueEnabled = pickerDate !== "";

  const openPicker = () => {
    setDayDate(null);
    setPickerDate(today);
    setPickerOpen(true);
  };

  const submitPicker = (event: FormEvent) => {
    event.preventDefault();
    if (!continueEnabled) {
      return;
    }
    setPickerOpen(false);
    setDayDate(pickerDate);
  };

  return (
    <section className="card archive-card">
      <div className="archive-card-head">
        <h2>Watch diary</h2>
        <GearButton label="Edit a day" onClick={openPicker} />
      </div>
      <div className="archive-month-nav">
        <button
          type="button"
          className="linkish"
          aria-label="Previous month"
          onClick={() => goMonth(previous)}
        >
          ← {formatMonthLabel(previous)}
        </button>
        <span className="archive-month-label">{formatMonthLabel(selected)}</span>
        <button type="button" className="linkish" aria-label="Next month" onClick={() => goMonth(next)}>
          {formatMonthLabel(next)} →
        </button>
      </div>

      {unauthorized ? (
        <p className="hint">Returning to sign in...</p>
      ) : archiveQuery.isPending ? (
        <p className="hint">Loading diary...</p>
      ) : archiveQuery.isError ? (
        <>
          <p className="status-note">Could not load diary.</p>
          <button type="button" className="linkish" onClick={() => void archiveQuery.refetch()}>
            Retry
          </button>
        </>
      ) : !archiveQuery.data || archiveQuery.data.events.length === 0 ? (
        <p className="hint">No watches this month.</p>
      ) : (
        groupWatchEventsByDay(archiveQuery.data.events).map((group) => (
          <div key={group.watchedOn} className="archive-day">
            <div className="archive-day-head">
              <h3>{formatDayHeading(group.watchedOn)}</h3>
              <GearButton
                label={`Edit watches for ${group.watchedOn}`}
                onClick={() => {
                  setPickerOpen(false);
                  setDayDate(group.watchedOn);
                }}
              />
            </div>
            <ul className="event-list">
              {group.events.map((event) => (
                <li key={event.id}>{event.contentTitle}</li>
              ))}
            </ul>
          </div>
        ))
      )}

      {pickerOpen ? (
        <OverlayDialog labelledBy={PICKER_TITLE_ID} onClose={() => setPickerOpen(false)} isTop={!dayDate}>
          <h2 id={PICKER_TITLE_ID}>Edit a day</h2>
          <form onSubmit={submitPicker}>
            <label htmlFor="diary-edit-date">Date</label>
            <input
              id="diary-edit-date"
              type="date"
              value={pickerDate}
              onChange={(event) => setPickerDate(event.target.value)}
              required
            />
            <div className="dialog-actions">
              <button type="button" className="linkish" onClick={() => setPickerOpen(false)}>
                Cancel
              </button>
              <button type="submit" disabled={!continueEnabled}>
                Continue
              </button>
            </div>
          </form>
        </OverlayDialog>
      ) : null}

      {dayDate ? <ArchiveDayDialog date={dayDate} onClose={() => setDayDate(null)} /> : null}
    </section>
  );
}
