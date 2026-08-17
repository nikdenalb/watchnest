import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { addPlanTodayLine, deletePlanTodayLine, patchPlanTodayLine } from "./api/planner";
import { isApiError } from "./api/errors";
import { clearUserScopedQueries, invalidatePlanQueries } from "./session";
import type { PlanToday } from "./types";

export function PlanTodaySection({ planToday }: { planToday: PlanToday }) {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");

  const onAuthError = (error: unknown) => {
    if (isApiError(error) && error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  };

  const addMutation = useMutation({
    mutationFn: addPlanTodayLine,
    onSuccess: () => {
      setTitle("");
      invalidatePlanQueries(queryClient);
    },
    onError: onAuthError,
  });

  const patchMutation = useMutation({
    mutationFn: ({ id, checked }: { id: string; checked: boolean }) => patchPlanTodayLine(id, checked),
    onSuccess: () => invalidatePlanQueries(queryClient),
    onError: onAuthError,
  });

  const deleteMutation = useMutation({
    mutationFn: deletePlanTodayLine,
    onSuccess: () => invalidatePlanQueries(queryClient),
    onError: onAuthError,
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const contentTitle = title.trim();
    if (!contentTitle) {
      return;
    }
    addMutation.mutate(contentTitle);
  };

  return (
    <article className="card">
      <h2>Plan today</h2>
      <p className="hint">Checked titles move to watch history when the day rolls.</p>
      {planToday.lines.length === 0 ? (
        <p className="hint">No titles planned for today.</p>
      ) : (
        <ul className="event-list plan-list">
          {planToday.lines.map((line) => (
            <li key={line.id} className="plan-line">
              <label className="plan-line-check">
                <input
                  type="checkbox"
                  checked={line.checked}
                  disabled={patchMutation.isPending && patchMutation.variables?.id === line.id}
                  onChange={(event) =>
                    patchMutation.mutate({ id: line.id, checked: event.target.checked })
                  }
                />
                <span>{line.contentTitle}</span>
              </label>
              <button
                type="button"
                className="linkish"
                aria-label={`Remove ${line.contentTitle}`}
                disabled={deleteMutation.isPending && deleteMutation.variables === line.id}
                onClick={() => deleteMutation.mutate(line.id)}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
      <form onSubmit={submit}>
        <label htmlFor="plan-today-title">What to watch today?</label>
        <input
          id="plan-today-title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Episode title"
          maxLength={120}
          required
        />
        <button type="submit" disabled={addMutation.isPending}>
          Add to today
        </button>
      </form>
    </article>
  );
}
