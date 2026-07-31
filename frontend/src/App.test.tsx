import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import type { Dashboard } from "./types";

const dashboard: Dashboard = {
  displayName: "You",
  today: "2026-07-27",
  status: {
    date: "2026-07-27",
    episodeLimit: 2,
    episodesWatched: 1,
    episodesRemaining: 1,
    overQuota: false,
    canWatchAnotherEpisode: true,
  },
  policy: {
    weekdayEpisodeLimit: 2,
    weekendEpisodeLimit: 4,
  },
  todayEvents: [
    {
      id: "evt-1",
      ownerId: "owner",
      watchedOn: "2026-07-27",
      contentTitle: "Pilot",
    },
  ],
};

type FetchImpl = (input: RequestInfo | URL, init?: RequestInit) => Promise<{
  ok: boolean;
  status?: number;
  json: () => Promise<unknown>;
}>;

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return render(<App />, { wrapper });
}

describe("App", () => {
  let fetchImpl: FetchImpl;

  beforeEach(() => {
    fetchImpl = async () => ({
      ok: true,
      json: async () => dashboard,
    });
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL, init?: RequestInit) => fetchImpl(input, init)),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  async function dismissSplash() {
    const user = userEvent.setup();
    await screen.findByLabelText(/WatchNest splash screen/i);
    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Press any key or click to continue/i })).toBeInTheDocument();
    });
    await user.keyboard("{Enter}");
  }

  it("shows the splash until dismissed, then the dashboard", async () => {
    renderApp();

    expect(await screen.findByText("WatchNest")).toBeInTheDocument();
    expect(screen.queryByText("Your watch day")).not.toBeInTheDocument();

    await dismissSplash();

    expect(await screen.findByRole("heading", { name: "Your watch day" })).toBeInTheDocument();
    expect(screen.getByText("Today: 2026-07-27")).toBeInTheDocument();
    expect(screen.getByText("Pilot")).toBeInTheDocument();
    expect(screen.getByText("Another episode fits today's limit.")).toBeInTheDocument();
  });

  it("shows a failure state when the dashboard cannot load", async () => {
    fetchImpl = async () => ({
      ok: false,
      status: 503,
      json: async () => ({}),
    });

    renderApp();
    await dismissSplash();

    expect(await screen.findByText("Failed to load library dashboard.")).toBeInTheDocument();
  });

  it("logs a watch and refreshes the dashboard", async () => {
    fetchImpl = async (input, init) => {
      const url = String(input);
      if (url.includes("/watch-events") && init?.method === "POST") {
        return {
          ok: true,
          json: async () => ({
            id: "evt-2",
            ownerId: "owner",
            watchedOn: "2026-07-27",
            contentTitle: "Episode 2",
          }),
        };
      }
      if (url.includes("/dashboard")) {
        return {
          ok: true,
          json: async () => ({
            ...dashboard,
            todayEvents: [
              ...dashboard.todayEvents,
              {
                id: "evt-2",
                ownerId: "owner",
                watchedOn: "2026-07-27",
                contentTitle: "Episode 2",
              },
            ],
            status: {
              ...dashboard.status,
              episodesWatched: 2,
              episodesRemaining: 0,
              canWatchAnotherEpisode: false,
            },
          }),
        };
      }
      return { ok: false, status: 404, json: async () => ({}) };
    };

    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await screen.findByRole("heading", { name: "Your watch day" });
    await user.type(screen.getByLabelText("What was watched?"), "Episode 2");
    await user.click(screen.getByRole("button", { name: "Add to watch log" }));

    await waitFor(() => {
      expect(screen.getByText("Episode 2")).toBeInTheDocument();
      expect(screen.getByText("Today's limit is reached.")).toBeInTheDocument();
    });
  });

  it("saves screen-time rules", async () => {
    fetchImpl = async (input, init) => {
      const url = String(input);
      if (url.includes("/policy") && init?.method === "PUT") {
        return {
          ok: true,
          json: async () => ({
            weekdayEpisodeLimit: 3,
            weekendEpisodeLimit: 5,
          }),
        };
      }
      if (url.includes("/dashboard")) {
        return {
          ok: true,
          json: async () => ({
            ...dashboard,
            policy: {
              weekdayEpisodeLimit: 3,
              weekendEpisodeLimit: 5,
            },
            status: {
              ...dashboard.status,
              episodeLimit: 3,
              episodesRemaining: 2,
            },
          }),
        };
      }
      return { ok: false, status: 404, json: async () => ({}) };
    };

    const user = userEvent.setup();
    renderApp();
    await dismissSplash();
    await screen.findByRole("heading", { name: "Your watch day" });

    const weekday = screen.getByLabelText("Weekday limit");
    const weekend = screen.getByLabelText("Weekend limit");
    await user.clear(weekday);
    await user.type(weekday, "3");
    await user.clear(weekend);
    await user.type(weekend, "5");
    await user.click(screen.getByRole("button", { name: "Save rules" }));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "/api/v1/policy",
        expect.objectContaining({
          method: "PUT",
          body: JSON.stringify({
            weekdayEpisodeLimit: 3,
            weekendEpisodeLimit: 5,
          }),
        }),
      );
    });

    await waitFor(() => {
      const quota = screen.getByRole("heading", { name: "Screen time today" }).closest("section");
      expect(quota).not.toBeNull();
      expect(within(quota as HTMLElement).getByText("3")).toBeInTheDocument();
    });
  });
});
