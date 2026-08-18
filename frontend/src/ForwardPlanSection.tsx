import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState } from "react";
import { addForwardPlanItem, deleteForwardPlanItem, fetchForwardPlan } from "./api/planner";
import { isApiError } from "./api/errors";
import { formatDayHeading, formatMonthLabel, yearMonthFromIso } from "./archiveMonthRange";
import {
  addDays,
  forwardDisplayRange,
  planAddTarget,
  shiftForwardAnchor,
  type ForwardDisplayMode,
} from "./forwardPlanRange";
import { clearUserScopedQueries, forwardPlanQueryKey, invalidatePlanQueries } from "./session";
import type { ForwardPlanItem } from "./types";

const MODES: { id: ForwardDisplayMode; label: string }[] = [
  { id: "week", label: "Week" },
  { id: "month", label: "Month" },
  { id: "year", label: "Year" },
];

export function groupForwardItemsByDate(
  items: ForwardPlanItem[],
): { plannedFor: string; items: ForwardPlanItem[] }[] {
  const groups: { plannedFor: string; items: ForwardPlanItem[] }[] = [];
  for (const item of items) {
    const last = groups.at(-1);
    if (last && last.plannedFor === item.plannedFor) {
      last.items.push(item);
    } else {
      groups.push({ plannedFor: item.plannedFor, items: [item] });
    }
  }
  return groups;
}

function rangeNavLabel(mode: ForwardDisplayMode, direction: "previous" | "next"): string {
  const unit = mode === "week" ? "week" : mode === "month" ? "month" : "year";
  return `${direction === "previous" ? "Previous" : "Next"} ${unit}`;
}

function formatRangeCaption(from: string, to: string, mode: ForwardDisplayMode): string {
  if (mode === "year") {
    return from.slice(0, 4);
  }
  if (mode === "month") {
    return formatMonthLabel(yearMonthFromIso(from));
  }
  return `${formatDayHeading(from)} – ${formatDayHeading(to)}`;
}

export function ForwardPlanSection({ today }: { today: string }) {
  const queryClient = useQueryClient();
  const tomorrow = addDays(today, 1);
  const [mode, setMode] = useState<ForwardDisplayMode>("week");
  const [anchor, setAnchor] = useState(today);
  const [plannedFor, setPlannedFor] = useState(tomorrow);
  const [title, setTitle] = useState("");

  useEffect(() => {
    setAnchor(today);
    setPlannedFor((current) => (current <= today ? addDays(today, 1) : current));
  }, [today]);

  const { from, to } = forwardDisplayRange(anchor, mode);

  const planQuery = useQuery({
    queryKey: forwardPlanQueryKey(from, to),
    queryFn: () => fetchForwardPlan(from, to),
    retry: false,
  });

  useEffect(() => {
    if (isApiError(planQuery.error) && planQuery.error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  }, [planQuery.error, queryClient]);

  const onAuthError = (error: unknown) => {
    if (isApiError(error) && error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  };

  const addMutation = useMutation({
    mutationFn: async ({ date, contentTitle }: { date: string; contentTitle: string }) => {
      if (planAddTarget(today, date) !== "forward") {
        return;
      }
      await addForwardPlanItem(date, contentTitle);
    },
    onSuccess: () => {
      setTitle("");
      invalidatePlanQueries(queryClient);
    },
    onError: onAuthError,
  });

  const deleteMutation = useMutation({
    mutationFn: deleteForwardPlanItem,
    onSuccess: () => invalidatePlanQueries(queryClient),
    onError: onAuthError,
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const contentTitle = title.trim();
    if (!contentTitle) {
      return;
    }
    if (planAddTarget(today, plannedFor) !== "forward") {
      return;
    }
    addMutation.mutate({ date: plannedFor, contentTitle });
  };

  const unauthorized = isApiError(planQuery.error) && planQuery.error.status === 401;

  return (
    <section className="card archive-card">
      <h2>Forward plan</h2>
      <p className="hint">Week, month, and year change the dates shown, not the plan itself.</p>
      <div className="plan-mode-row" role="group" aria-label="Forward plan range">
        {MODES.map((entry) => (
          <button
            key={entry.id}
            type="button"
            className={mode === entry.id ? "auth-mode is-active" : "auth-mode"}
            aria-pressed={mode === entry.id}
            onClick={() => setMode(entry.id)}
          >
            {entry.label}
          </button>
        ))}
      </div>
      <div className="archive-month-nav">
        <button
          type="button"
          className="linkish"
          aria-label={rangeNavLabel(mode, "previous")}
          onClick={() => setAnchor(shiftForwardAnchor(anchor, mode, -1))}
        >
          ← Previous
        </button>
        <span className="archive-month-label">{formatRangeCaption(from, to, mode)}</span>
        <button
          type="button"
          className="linkish"
          aria-label={rangeNavLabel(mode, "next")}
          onClick={() => setAnchor(shiftForwardAnchor(anchor, mode, 1))}
        >
          Next →
        </button>
      </div>

      {unauthorized ? (
        <p className="hint">Returning to sign in...</p>
      ) : planQuery.isPending ? (
        <p className="hint">Loading forward plan...</p>
      ) : planQuery.isError ? (
        <>
          <p className="status-note">Could not load forward plan.</p>
          <button type="button" className="linkish" onClick={() => void planQuery.refetch()}>
            Retry
          </button>
        </>
      ) : !planQuery.data || planQuery.data.items.length === 0 ? (
        <p className="hint">No titles planned in this range.</p>
      ) : (
        groupForwardItemsByDate(planQuery.data.items).map((group) => (
          <div key={group.plannedFor} className="archive-day">
            <h3>{formatDayHeading(group.plannedFor)}</h3>
            <ul className="event-list plan-list">
              {group.items.map((item) => (
                <li key={item.id} className="plan-line">
                  <span>{item.contentTitle}</span>
                  {item.plannedFor > today ? (
                    <button
                      type="button"
                      className="linkish"
                      aria-label={`Remove ${item.contentTitle}`}
                      disabled={deleteMutation.isPending && deleteMutation.variables === item.id}
                      onClick={() => deleteMutation.mutate(item.id)}
                    >
                      Remove
                    </button>
                  ) : null}
                </li>
              ))}
            </ul>
          </div>
        ))
      )}

      <form onSubmit={submit}>
        <div className="forward-add-row">
          <div>
            <label htmlFor="forward-plan-date">Plan for</label>
            <input
              id="forward-plan-date"
              type="date"
              min={tomorrow}
              value={plannedFor}
              onChange={(event) => setPlannedFor(event.target.value)}
              required
            />
          </div>
          <div>
            <label htmlFor="forward-plan-title">Forward title</label>
            <input
              id="forward-plan-title"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Episode title"
              maxLength={120}
              required
            />
          </div>
        </div>
        <button type="submit" disabled={addMutation.isPending}>
          Add to plan
        </button>
      </form>
    </section>
  );
}
