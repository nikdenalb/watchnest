import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useEffect, useState, type ReactNode } from "react";
import { logout } from "./api/auth";
import { isApiError } from "./api/errors";
import { createTitle, deleteTitle, fetchTitles, updateTitle } from "./api/titles";
import { ConfirmDialog } from "./ConfirmDialog";
import { TitleForm } from "./TitleForm";
import { clearCmsScopedQueries, invalidateTitlesQueries, titlesQueryKey } from "./session";
import type { CmsUser, TitleWrite } from "./types";

function PageShell({
  children,
  username,
  onLogout,
  logoutPending,
}: {
  children: ReactNode;
  username: string;
  onLogout: () => void;
  logoutPending: boolean;
}) {
  return (
    <main className="page app-enter">
      <div className="page-top">
        <p className="eyebrow">WatchNest CMS</p>
        <div className="session-bar">
          <span className="session-user">{username}</span>
          <button type="button" className="linkish" onClick={onLogout} disabled={logoutPending}>
            Log out
          </button>
        </div>
      </div>
      {children}
    </main>
  );
}

function useClearOnUnauthorized(error: unknown) {
  const queryClient = useQueryClient();
  useEffect(() => {
    if (isApiError(error) && error.status === 401) {
      clearCmsScopedQueries(queryClient);
    }
  }, [error, queryClient]);
}

export function CatalogEditor({ user }: { user: CmsUser }) {
  const queryClient = useQueryClient();
  const [searchDraft, setSearchDraft] = useState("");
  const [appliedQuery, setAppliedQuery] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const titlesQuery = useQuery({
    queryKey: titlesQueryKey(appliedQuery),
    queryFn: () => fetchTitles(appliedQuery),
    retry: false,
  });

  useClearOnUnauthorized(titlesQuery.error);

  const titles = titlesQuery.data?.titles ?? [];
  const selected = titles.find((title) => title.id === selectedId);

  const createMutation = useMutation({
    mutationFn: createTitle,
    onSuccess: () => {
      invalidateTitlesQueries(queryClient);
    },
    onError: (error) => {
      if (isApiError(error) && error.status === 401) {
        clearCmsScopedQueries(queryClient);
      }
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, fields }: { id: string; fields: TitleWrite }) => updateTitle(id, fields),
    onSuccess: () => {
      invalidateTitlesQueries(queryClient);
    },
    onError: (error) => {
      if (isApiError(error) && error.status === 401) {
        clearCmsScopedQueries(queryClient);
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteTitle,
    onSuccess: () => {
      setConfirmDelete(false);
      setSelectedId(null);
      invalidateTitlesQueries(queryClient);
    },
    onError: (error) => {
      if (isApiError(error) && error.status === 401) {
        clearCmsScopedQueries(queryClient);
      }
    },
  });

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearCmsScopedQueries(queryClient);
    },
  });

  const shell = {
    username: user.username,
    onLogout: () => logoutMutation.mutate(),
    logoutPending: logoutMutation.isPending,
  };

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    setAppliedQuery(searchDraft.trim());
    setSelectedId(null);
    createMutation.reset();
    updateMutation.reset();
  };

  const formPending = createMutation.isPending || updateMutation.isPending || deleteMutation.isPending;

  if (isApiError(titlesQuery.error) && titlesQuery.error.status === 401) {
    return (
      <PageShell {...shell}>
        <p>Returning to sign in...</p>
      </PageShell>
    );
  }

  if (titlesQuery.isPending) {
    return (
      <PageShell {...shell}>
        <p>Loading titles...</p>
      </PageShell>
    );
  }

  if (titlesQuery.isError) {
    return (
      <PageShell {...shell}>
        <p>Failed to load titles.</p>
      </PageShell>
    );
  }

  return (
    <PageShell {...shell}>
      <header className="hero">
        <h1>Catalog</h1>
        <p className="subtitle">Owned titles. Search matches English name.</p>
      </header>

      <section className="card">
        <h2>Search</h2>
        <form onSubmit={submitSearch}>
          <label htmlFor="titleSearch">Search English name</label>
          <input
            id="titleSearch"
            name="q"
            type="search"
            value={searchDraft}
            onChange={(event) => setSearchDraft(event.target.value)}
            disabled={formPending}
          />
          <button type="submit" disabled={formPending}>
            Search
          </button>
        </form>
      </section>

      <section className="grid">
        <article className="card">
          <div className="card-head">
            <h2>Titles</h2>
            {selected ? (
              <button
                type="button"
                className="linkish"
                onClick={() => {
                  setSelectedId(null);
                  updateMutation.reset();
                  createMutation.reset();
                }}
              >
                New title
              </button>
            ) : null}
          </div>
          {titles.length === 0 ? (
            <p className="hint">
              {appliedQuery ? "No titles match the search." : "No titles in the catalog."}
            </p>
          ) : (
            <ul className="title-list">
              {titles.map((title) => (
                <li key={title.id}>
                  <button
                    type="button"
                    className={title.id === selectedId ? "title-pick is-selected" : "title-pick"}
                    aria-pressed={title.id === selectedId}
                    onClick={() => {
                      setSelectedId(title.id);
                      createMutation.reset();
                      updateMutation.reset();
                      setConfirmDelete(false);
                    }}
                  >
                    <span className="title-pick-name">{title.nameEn}</span>
                    <span className="title-pick-meta">
                      {title.year} · {title.type}
                      {title.nameOriginal && title.nameOriginal !== title.nameEn
                        ? ` · ${title.nameOriginal}`
                        : ""}
                    </span>
                    {title.genres ? <span className="title-pick-tags">{title.genres}</span> : null}
                    {title.countries ? <span className="title-pick-tags">{title.countries}</span> : null}
                    {title.description ? (
                      <span className="title-pick-tags">{title.description}</span>
                    ) : null}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </article>

        {selected ? (
          <TitleForm
            key={selected.id}
            title={selected}
            pending={formPending}
            error={updateMutation.error ?? deleteMutation.error}
            onSubmit={(fields) => updateMutation.mutate({ id: selected.id, fields })}
            onDelete={() => setConfirmDelete(true)}
          />
        ) : (
          <TitleForm
            key={`create-${createMutation.data?.id ?? "new"}`}
            pending={formPending}
            error={createMutation.error}
            onSubmit={(fields) => createMutation.mutate(fields)}
          />
        )}
      </section>

      {confirmDelete && selected ? (
        <ConfirmDialog
          title="Delete title"
          titleId="delete-title-heading"
          message={`Delete “${selected.nameEn}” (${selected.year}, ${selected.type})? This cannot be undone.`}
          confirmLabel="Delete"
          pending={deleteMutation.isPending}
          onConfirm={() => deleteMutation.mutate(selected.id)}
          onClose={() => setConfirmDelete(false)}
        />
      ) : null}
    </PageShell>
  );
}
