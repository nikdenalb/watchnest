import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
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
  return { ...render(<WatchArchiveSection today={today} />, { wrapper }), queryClient };
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

  it("puts a gear on past days and the header, not on a today-dated group", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(jsonResponse({ from: "2026-08-01", to: "2026-08-14", events: augustEvents })),
    );

    renderArchive();
    expect(await screen.findByText("Feature film")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Correct a day" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Correct watches for 2026-08-12" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Correct watches for 2026-08-14" })).not.toBeInTheDocument();
  });

  it("keeps the header gear when the month is empty", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(jsonResponse({ from: "2026-08-01", to: "2026-08-14", events: [] })),
    );

    renderArchive();
    expect(await screen.findByText("No watches logged this month.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Correct a day" })).toBeInTheDocument();
  });

  it("opens a past-day dialog that owns the single-day GET and leaves the diary without Remove", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/auth/csrf")) {
        return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
      }
      if (url.includes("/watch-events") && method === "GET") {
        const parsed = new URL(url, "http://localhost");
        const from = parsed.searchParams.get("from") ?? "";
        const to = parsed.searchParams.get("to") ?? "";
        const events = augustEvents.filter((event) => event.watchedOn >= from && event.watchedOn <= to);
        return jsonResponse({ from, to, events });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    const user = userEvent.setup();
    renderArchive();
    expect(await screen.findByText("Feature film")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Correct watches for 2026-08-12" }));
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByRole("heading", { name: "Correct watches 12 Aug" })).toBeInTheDocument();
    expect(within(dialog).getByText("Feature film")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Remove/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("dialog", { hidden: false })).toBeTruthy();
    expect(document.querySelector("dialog")).toBeNull();

    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(
          ([input, init]) =>
            String(input) === "/api/v1/watch-events?from=2026-08-12&to=2026-08-12" &&
            (init?.method ?? "GET").toUpperCase() === "GET",
        ),
      ).toBe(true);
    });
  });

  it("adds, renames, and deletes from the day dialog and invalidates archive plus dashboard", async () => {
    let events: WatchEvent[] = structuredClone(augustEvents);
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/auth/csrf")) {
        return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
      }
      if (url.includes("/watch-events/") && method === "PATCH") {
        const id = url.split("/watch-events/")[1];
        const body = JSON.parse(String(init?.body ?? "{}")) as { contentTitle?: string };
        events = events.map((event) =>
          event.id === id ? { ...event, contentTitle: body.contentTitle ?? event.contentTitle } : event,
        );
        return jsonResponse(events.find((event) => event.id === id));
      }
      if (url.includes("/watch-events/") && method === "DELETE") {
        const id = url.split("/watch-events/")[1];
        events = events.filter((event) => event.id !== id);
        return { ok: true, status: 204, json: async () => ({}) };
      }
      if (url.includes("/watch-events") && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as {
          watchedOn?: string;
          contentTitle?: string;
        };
        const created: WatchEvent = {
          id: `new-${events.length + 1}`,
          ownerId: "user-a",
          watchedOn: body.watchedOn ?? "",
          contentTitle: body.contentTitle ?? "",
        };
        events = [...events, created];
        return jsonResponse(created, 201);
      }
      if (url.includes("/watch-events") && method === "GET") {
        const parsed = new URL(url, "http://localhost");
        const from = parsed.searchParams.get("from") ?? "";
        const to = parsed.searchParams.get("to") ?? "";
        return jsonResponse({
          from,
          to,
          events: events.filter((event) => event.watchedOn >= from && event.watchedOn <= to),
        });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    const user = userEvent.setup();
    const { queryClient } = renderArchive();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    expect(await screen.findByText("Feature film")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Correct watches for 2026-08-12" }));
    const dayDialog = await screen.findByRole("dialog");

    await user.type(within(dayDialog).getByLabelText("Title"), "Extra episode");
    await user.click(within(dayDialog).getByRole("button", { name: "Add" }));
    expect(await within(dayDialog).findByText("Extra episode")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([input, init]) =>
          String(input) === "/api/v1/watch-events" &&
          (init?.method ?? "").toUpperCase() === "POST" &&
          String(init?.body).includes("2026-08-12") &&
          String(init?.body).includes("Extra episode"),
      ),
    ).toBe(true);

    const filmRow = within(dayDialog).getByText("Feature film").closest("li") as HTMLElement;
    await user.click(within(filmRow).getByRole("button", { name: "Rename" }));
    const renameDialog = screen.getByRole("heading", { name: "Rename title" }).closest("[role=dialog]");
    expect(renameDialog).not.toBeNull();
    await user.clear(within(renameDialog as HTMLElement).getByLabelText("New title"));
    await user.type(within(renameDialog as HTMLElement).getByLabelText("New title"), "Renamed film");
    await user.click(within(renameDialog as HTMLElement).getByRole("button", { name: "Save" }));
    expect(await within(dayDialog).findByText("Renamed film")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([input, init]) =>
          String(input) === "/api/v1/watch-events/a1" && (init?.method ?? "").toUpperCase() === "PATCH",
      ),
    ).toBe(true);

    const renamedRow = within(dayDialog).getByText("Renamed film").closest("li") as HTMLElement;
    await user.click(within(renamedRow).getByRole("button", { name: "Delete" }));
    expect(
      fetchMock.mock.calls.some(
        ([input, init]) =>
          String(input).includes("/watch-events/") && (init?.method ?? "").toUpperCase() === "DELETE",
      ),
    ).toBe(false);
    const confirmDialog = screen.getByRole("heading", { name: "Delete this title?" }).closest("[role=dialog]");
    expect(confirmDialog).not.toBeNull();
    await user.click(within(confirmDialog as HTMLElement).getByRole("button", { name: "Delete" }));
    await waitFor(() => {
      expect(within(dayDialog).queryByText("Renamed film")).not.toBeInTheDocument();
    });
    expect(
      fetchMock.mock.calls.some(
        ([input, init]) =>
          String(input) === "/api/v1/watch-events/a1" && (init?.method ?? "").toUpperCase() === "DELETE",
      ),
    ).toBe(true);

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["watch-events"] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] });
    expect(invalidate.mock.calls.some((call) => call[0]?.queryKey?.[0] === "plan-forward")).toBe(false);
  });

  it("opens yesterday from the header picker and ignores today", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/watch-events") && method === "GET") {
        const parsed = new URL(url, "http://localhost");
        const from = parsed.searchParams.get("from") ?? "";
        const to = parsed.searchParams.get("to") ?? "";
        const events = augustEvents.filter((event) => event.watchedOn >= from && event.watchedOn <= to);
        return jsonResponse({ from, to, events });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    const user = userEvent.setup();
    renderArchive();
    expect(await screen.findByText("Feature film")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Correct a day" }));
    const picker = screen.getByRole("heading", { name: "Correct a day" }).closest("[role=dialog]");
    expect(picker).not.toBeNull();
    const dateInput = within(picker as HTMLElement).getByLabelText("Date");
    expect(dateInput).toHaveAttribute("max", "2026-08-13");
    expect(dateInput).toHaveValue("2026-08-13");

    await user.click(within(picker as HTMLElement).getByRole("button", { name: "Continue" }));
    expect(await screen.findByRole("heading", { name: "Correct watches 13 Aug" })).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([input]) => String(input) === "/api/v1/watch-events?from=2026-08-13&to=2026-08-13",
      ),
    ).toBe(true);

    await user.keyboard("{Escape}");
    await waitFor(() => {
      expect(screen.queryByRole("heading", { name: "Correct watches 13 Aug" })).not.toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Correct a day" }));
    const pickerAgain = screen.getByRole("heading", { name: "Correct a day" }).closest("[role=dialog]");
    const continueBtn = within(pickerAgain as HTMLElement).getByRole("button", { name: "Continue" });
    fireEvent.change(within(pickerAgain as HTMLElement).getByLabelText("Date"), {
      target: { value: "2026-08-14" },
    });
    expect(continueBtn).toBeDisabled();
    expect(screen.queryByRole("heading", { name: "Correct watches 14 Aug" })).not.toBeInTheDocument();
  });

  it("closes only the top nested dialog on Escape", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        const method = (init?.method ?? "GET").toUpperCase();
        if (url.includes("/watch-events") && method === "GET") {
          const parsed = new URL(url, "http://localhost");
          const from = parsed.searchParams.get("from") ?? "";
          const to = parsed.searchParams.get("to") ?? "";
          const events = augustEvents.filter((event) => event.watchedOn >= from && event.watchedOn <= to);
          return jsonResponse({ from, to, events });
        }
        return jsonResponse({}, 404);
      }),
    );

    const user = userEvent.setup();
    renderArchive();
    expect(await screen.findByText("Feature film")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Correct watches for 2026-08-12" }));
    const dayDialog = await screen.findByRole("dialog");
    await user.click(within(dayDialog).getByRole("button", { name: "Rename" }));
    expect(screen.getByRole("heading", { name: "Rename title" })).toBeInTheDocument();

    await user.keyboard("{Escape}");
    expect(screen.queryByRole("heading", { name: "Rename title" })).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Correct watches 12 Aug" })).toBeInTheDocument();
  });
});
