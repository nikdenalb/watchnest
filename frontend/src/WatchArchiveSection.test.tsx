import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearCsrfCache } from "./api/http";
import type { WatchEvent, WatchEventArchive } from "./types";
import { groupWatchEventsByDay, WatchArchiveSection } from "./WatchArchiveSection";

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

function renderArchive(today = "2026-08-14") {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return render(<WatchArchiveSection today={today} />, { wrapper });
}

const augustEvents: WatchEvent[] = [
  { id: "a2", ownerId: "user-a", watchedOn: "2026-08-14", contentTitle: "Blue Tractor" },
  { id: "a1", ownerId: "user-a", watchedOn: "2026-08-12", contentTitle: "Feature film" },
];

const julyEvents: WatchEvent[] = [
  { id: "j1", ownerId: "user-a", watchedOn: "2026-07-06", contentTitle: "July episode" },
];

describe("groupWatchEventsByDay", () => {
  it("groups consecutive same-day titles and keeps day order", () => {
    const grouped = groupWatchEventsByDay([
      { id: "1", ownerId: "a", watchedOn: "2026-08-14", contentTitle: "Blue Tractor" },
      { id: "2", ownerId: "a", watchedOn: "2026-08-14", contentTitle: "Another episode" },
      { id: "3", ownerId: "a", watchedOn: "2026-08-12", contentTitle: "Feature film" },
    ]);
    expect(grouped).toEqual([
      {
        watchedOn: "2026-08-14",
        events: [
          { id: "1", ownerId: "a", watchedOn: "2026-08-14", contentTitle: "Blue Tractor" },
          { id: "2", ownerId: "a", watchedOn: "2026-08-14", contentTitle: "Another episode" },
        ],
      },
      {
        watchedOn: "2026-08-12",
        events: [{ id: "3", ownerId: "a", watchedOn: "2026-08-12", contentTitle: "Feature film" }],
      },
    ]);
  });
});

describe("WatchArchiveSection", () => {
  beforeEach(() => {
    clearCsrfCache();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  it("shows loading then grouped days for the current month", async () => {
    let resolveArchive: ((value: WatchEventArchive) => void) | undefined;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        expect((init?.method ?? "GET").toUpperCase()).toBe("GET");
        expect(String(input)).toContain("/api/v1/watch-events?from=2026-08-01&to=2026-08-14");
        const body = await new Promise<WatchEventArchive>((resolve) => {
          resolveArchive = resolve;
        });
        return jsonResponse(body);
      }),
    );

    renderArchive();
    expect(screen.getByRole("heading", { name: "Watch history" })).toBeInTheDocument();
    expect(screen.getByText("Loading watch history...")).toBeInTheDocument();
    expect(screen.getByText("August 2026")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next month" })).toBeDisabled();

    resolveArchive?.({ from: "2026-08-01", to: "2026-08-14", events: augustEvents });

    expect(await screen.findByRole("heading", { name: "14 Aug" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "12 Aug" })).toBeInTheDocument();
    expect(screen.getByText("Blue Tractor")).toBeInTheDocument();
    expect(screen.getByText("Feature film")).toBeInTheDocument();
  });

  it("shows an empty-month hint, not an error", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse({ from: "2026-08-01", to: "2026-08-14", events: [] }),
      ),
    );

    renderArchive();
    expect(await screen.findByText("No watches logged this month.")).toBeInTheDocument();
    expect(screen.queryByText("Could not load watch history.")).not.toBeInTheDocument();
  });

  it("keeps quota-unrelated errors in the card and retries", async () => {
    let augustAttempts = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("from=2026-08-01")) {
        augustAttempts += 1;
        if (augustAttempts === 1) {
          return jsonResponse({ code: "request_failed", message: "boom" }, 500);
        }
        return jsonResponse({ from: "2026-08-01", to: "2026-08-14", events: augustEvents });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderArchive();
    expect(await screen.findByText("Could not load watch history.")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByText("Blue Tractor")).toBeInTheDocument();
  });

  it("does not keep the previous month list under a new month label", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      expect(method).toBe("GET");
      if (url.includes("from=2026-08-01")) {
        return jsonResponse({ from: "2026-08-01", to: "2026-08-14", events: augustEvents });
      }
      if (url.includes("from=2026-07-01")) {
        await new Promise((resolve) => setTimeout(resolve, 20));
        return jsonResponse({ from: "2026-07-01", to: "2026-07-31", events: julyEvents });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    renderArchive();
    expect(await screen.findByText("Blue Tractor")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Previous month" }));
    expect(screen.getByText("July 2026")).toBeInTheDocument();
    expect(screen.getByText("Loading watch history...")).toBeInTheDocument();
    expect(screen.queryByText("Blue Tractor")).not.toBeInTheDocument();
    expect(await screen.findByText("July episode")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Next month" })).not.toBeDisabled();
  });

  it("clears user-scoped queries on 401", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse({ code: "authentication_required", message: "Auth required" }, 401),
      ),
    );

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    queryClient.setQueryData(["me"], { id: "user-a", username: "alice" });
    queryClient.setQueryData(["dashboard"], { today: "2026-08-14" });

    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    render(<WatchArchiveSection today="2026-08-14" />, { wrapper });

    await waitFor(() => {
      expect(queryClient.getQueryData(["me"])).toBeNull();
    });
    expect(queryClient.getQueriesData({ queryKey: ["dashboard"] })).toEqual([]);
  });
});
