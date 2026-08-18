import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState } from "react";
import { isApiError } from "./api/errors";
import { addWatchEvent, deleteWatchEvent, fetchWatchEvents, patchWatchEvent } from "./api/planner";
import { formatDayHeading } from "./archiveMonthRange";
import { OverlayDialog } from "./OverlayDialog";
import {
  clearUserScopedQueries,
  invalidateArchiveQueries,
  watchEventsQueryKey,
} from "./session";
import type { WatchEvent } from "./types";

const DAY_TITLE_ID = "archive-day-dialog-title";
const RENAME_TITLE_ID = "archive-rename-dialog-title";
const DELETE_TITLE_ID = "archive-delete-dialog-title";

type Nested =
  | { kind: "rename"; event: WatchEvent }
  | { kind: "delete"; event: WatchEvent }
  | null;

export function ArchiveDayDialog({ date, onClose }: { date: string; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [renameTitle, setRenameTitle] = useState("");
  const [nested, setNested] = useState<Nested>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const dayQuery = useQuery({
    queryKey: watchEventsQueryKey(date, date),
    queryFn: () => fetchWatchEvents(date, date),
    retry: false,
  });

  useEffect(() => {
    if (isApiError(dayQuery.error) && dayQuery.error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  }, [dayQuery.error, queryClient]);

  const onAuthError = (error: unknown) => {
    if (isApiError(error) && error.status === 401) {
      clearUserScopedQueries(queryClient);
    }
  };

  const addMutation = useMutation({
    mutationFn: (contentTitle: string) => addWatchEvent(date, contentTitle),
    onSuccess: () => {
      setTitle("");
      setActionError(null);
      invalidateArchiveQueries(queryClient);
    },
    onError: (error) => {
      onAuthError(error);
      setActionError(isApiError(error) ? error.message : "Could not add the title.");
    },
  });

  const renameMutation = useMutation({
    mutationFn: ({ id, contentTitle }: { id: string; contentTitle: string }) =>
      patchWatchEvent(id, contentTitle),
    onSuccess: () => {
      setNested(null);
      setActionError(null);
      invalidateArchiveQueries(queryClient);
    },
    onError: (error) => {
      onAuthError(error);
      setActionError(isApiError(error) ? error.message : "Could not rename the title.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteWatchEvent(id),
    onSuccess: () => {
      setNested(null);
      setActionError(null);
      invalidateArchiveQueries(queryClient);
    },
    onError: (error) => {
      onAuthError(error);
      setActionError(isApiError(error) ? error.message : "Could not delete the title.");
    },
  });

  const pending =
    addMutation.isPending || renameMutation.isPending || deleteMutation.isPending;
  const unauthorized = isApiError(dayQuery.error) && dayQuery.error.status === 401;

  const submitAdd = (event: FormEvent) => {
    event.preventDefault();
    const contentTitle = title.trim();
    if (!contentTitle) {
      return;
    }
    addMutation.mutate(contentTitle);
  };

  const submitRename = (event: FormEvent) => {
    event.preventDefault();
    if (nested?.kind !== "rename") {
      return;
    }
    const contentTitle = renameTitle.trim();
    if (!contentTitle) {
      return;
    }
    renameMutation.mutate({ id: nested.event.id, contentTitle });
  };

  return (
    <>
      <OverlayDialog labelledBy={DAY_TITLE_ID} onClose={onClose} isTop={nested === null}>
        <div className="dialog-head">
          <h2 id={DAY_TITLE_ID}>
            Correct watches {formatDayHeading(date)}
          </h2>
          <button type="button" className="linkish" onClick={onClose}>
            Close
          </button>
        </div>

        {unauthorized ? (
          <p className="hint">Returning to sign in...</p>
        ) : dayQuery.isPending ? (
          <p className="hint">Loading watches...</p>
        ) : dayQuery.isError ? (
          <>
            <p className="status-note">
              {isApiError(dayQuery.error) ? dayQuery.error.message : "Could not load watches."}
            </p>
            <button type="button" className="linkish" onClick={() => void dayQuery.refetch()}>
              Retry
            </button>
          </>
        ) : (
          <ul className="event-list dialog-event-list">
            {(dayQuery.data?.events ?? []).map((item) => (
              <li key={item.id} className="dialog-event-row">
                <span>{item.contentTitle}</span>
                <button
                  type="button"
                  className="linkish"
                  disabled={pending}
                  onClick={() => {
                    setRenameTitle(item.contentTitle);
                    setNested({ kind: "rename", event: item });
                  }}
                >
                  Rename
                </button>
                <button
                  type="button"
                  className="linkish"
                  disabled={pending}
                  onClick={() => setNested({ kind: "delete", event: item })}
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}

        {actionError ? <p className="status-note">{actionError}</p> : null}

        <form onSubmit={submitAdd}>
          <label htmlFor="archive-add-title">Title</label>
          <input
            id="archive-add-title"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={120}
            required
          />
          <button type="submit" disabled={pending}>
            Add
          </button>
        </form>
      </OverlayDialog>

      {nested?.kind === "rename" ? (
        <OverlayDialog labelledBy={RENAME_TITLE_ID} onClose={() => setNested(null)} isTop>
          <h2 id={RENAME_TITLE_ID}>Rename title</h2>
          <form onSubmit={submitRename}>
            <label htmlFor="archive-rename-title">New title</label>
            <input
              id="archive-rename-title"
              value={renameTitle}
              onChange={(event) => setRenameTitle(event.target.value)}
              maxLength={120}
              required
            />
            <div className="dialog-actions">
              <button type="button" className="linkish" onClick={() => setNested(null)}>
                Cancel
              </button>
              <button type="submit" disabled={pending}>
                Save
              </button>
            </div>
          </form>
        </OverlayDialog>
      ) : null}

      {nested?.kind === "delete" ? (
        <OverlayDialog labelledBy={DELETE_TITLE_ID} onClose={() => setNested(null)} isTop>
          <h2 id={DELETE_TITLE_ID}>Delete this title?</h2>
          <p>{nested.event.contentTitle}</p>
          <div className="dialog-actions">
            <button type="button" className="linkish" onClick={() => setNested(null)}>
              Cancel
            </button>
            <button
              type="button"
              disabled={pending}
              className="dialog-danger"
              onClick={() => deleteMutation.mutate(nested.event.id)}
            >
              Delete
            </button>
          </div>
        </OverlayDialog>
      ) : null}
    </>
  );
}
