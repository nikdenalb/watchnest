import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { isApiError } from "./api/errors";
import { updateLibraryPreferences } from "./api/planner";
import { clearUserScopedQueries, invalidateLibraryRoots } from "./session";

export function AccountCard({ treatPlanAsWatched }: { treatPlanAsWatched: boolean }) {
  const queryClient = useQueryClient();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const preferenceMutation = useMutation({
    mutationFn: (next: boolean) => updateLibraryPreferences({ treatPlanAsWatched: next }),
    onSuccess: () => {
      setErrorMessage(null);
      invalidateLibraryRoots(queryClient);
    },
    onError: (error) => {
      if (isApiError(error) && error.status === 401) {
        clearUserScopedQueries(queryClient);
        return;
      }
      setErrorMessage(isApiError(error) ? error.message : "Could not save account preference.");
    },
  });

  return (
    <section className="card account-card">
      <h2>Account</h2>
      <label className="plan-line-check">
        <input
          type="checkbox"
          checked={treatPlanAsWatched}
          disabled={preferenceMutation.isPending}
          onChange={(event) => preferenceMutation.mutate(event.target.checked)}
        />
        <span>Treat planned titles as watched</span>
      </label>
      <p className="hint">
        When WatchNest moves to a new day, titles left on Plan Today and dated plans for missed
        days are added to watch history. Remove anything you did not watch.
      </p>
      {errorMessage ? <p className="status-note">{errorMessage}</p> : null}
    </section>
  );
}
