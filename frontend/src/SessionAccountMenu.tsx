import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { isApiError } from "./api/errors";
import { updateLibraryPreferences } from "./api/planner";
import { clearUserScopedQueries, invalidateLibraryRoots } from "./session";

const BUTTON_ID = "session-account-button";
const PANEL_ID = "session-account-panel";

export function SessionAccountMenu({
  username,
  treatPlanAsWatched,
  onLogout,
  logoutPending,
}: {
  username: string;
  treatPlanAsWatched?: boolean;
  onLogout: () => void;
  logoutPending: boolean;
}) {
  const queryClient = useQueryClient();
  const rootRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
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

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };
    const onPointerDown = (event: PointerEvent) => {
      const root = rootRef.current;
      if (root && event.target instanceof Node && !root.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("pointerdown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open]);

  return (
    <div className="session-bar" ref={rootRef}>
      <button
        type="button"
        id={BUTTON_ID}
        className="session-user"
        aria-expanded={open}
        aria-controls={open ? PANEL_ID : undefined}
        onClick={() => setOpen((current) => !current)}
      >
        {username}
      </button>
      {open ? (
        <div id={PANEL_ID} className="session-account-panel" aria-labelledby={BUTTON_ID}>
          {treatPlanAsWatched !== undefined ? (
            <>
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
            </>
          ) : null}
          <button type="button" className="linkish" onClick={onLogout} disabled={logoutPending}>
            Log out
          </button>
        </div>
      ) : null}
    </div>
  );
}
