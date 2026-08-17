import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearCsrfCache } from "./api/http";
import { Dashboard } from "./Dashboard";
import type { CurrentUser, Dashboard as DashboardData, ForwardPlanItem, PlanTodayLine, WatchEvent } from "./types";

const alice: CurrentUser = { id: "user-a", username: "alice" };

const aliceDashboard: DashboardData = {
  displayName: "alice",
  today: "2026-07-27",
  status: {
    date: "2026-07-27",
    episodeLimit: 2,
    episodesPlanned: 1,
    episodesRemaining: 1,
    overQuota: false,
    canAddAnotherEpisode: true,
  },
  policy: {
    weekdayEpisodeLimit: 2,
    weekendEpisodeLimit: 4,
  },
  planToday: {
    date: "2026-07-27",
    lines: [
      {
        id: "line-1",
        contentTitle: "Pilot",
        checked: false,
        source: "MANUAL",
      },
    ],
  },
};

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

function quotaFor(limit: number, planned: number) {
  return {
    episodesPlanned: planned,
    episodesRemaining: Math.max(0, limit - planned),
    overQuota: planned > limit,
    canAddAnotherEpisode: planned < limit,
  };
}

function renderDashboard(queryClient?: QueryClient) {
  const client =
    queryClient ??
    new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { ...render(<Dashboard user={alice} />, { wrapper }), queryClient: client };
}

describe("PlanTodaySection", () => {
  let dashboard: DashboardData;
  let events: WatchEvent[];
  let forwardItems: ForwardPlanItem[];

  beforeEach(() => {
    clearCsrfCache();
    dashboard = structuredClone(aliceDashboard);
    events = [];
    forwardItems = [];

    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        const method = (init?.method ?? "GET").toUpperCase();

        if (url.includes("/auth/csrf")) {
          return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
        }
        if (url.includes("/dashboard")) {
          return jsonResponse(dashboard);
        }
        if (url.includes("/watch-events") && method === "GET") {
          return jsonResponse({ from: "2026-07-01", to: "2026-07-27", events });
        }
        if (url.includes("/plan/forward") && method === "GET") {
          const parsed = new URL(url, "http://localhost");
          const from = parsed.searchParams.get("from") ?? "";
          const to = parsed.searchParams.get("to") ?? "";
          return jsonResponse({
            from,
            to,
            items: forwardItems.filter((item) => item.plannedFor >= from && item.plannedFor <= to),
          });
        }
        if (url.includes("/plan/today/lines") && method === "POST") {
          const body = JSON.parse(String(init?.body ?? "{}")) as { contentTitle?: string };
          const line: PlanTodayLine = {
            id: `line-${dashboard.planToday.lines.length + 1}`,
            contentTitle: body.contentTitle ?? "",
            checked: false,
            source: "MANUAL",
          };
          const lines = [...dashboard.planToday.lines, line];
          dashboard = {
            ...dashboard,
            planToday: { ...dashboard.planToday, lines },
            status: { ...dashboard.status, ...quotaFor(dashboard.status.episodeLimit, lines.length) },
          };
          return jsonResponse(line, 201);
        }
        if (url.includes("/plan/today/lines/") && method === "PATCH") {
          const id = url.split("/plan/today/lines/")[1];
          const body = JSON.parse(String(init?.body ?? "{}")) as { checked?: boolean };
          const lines = dashboard.planToday.lines.map((line) =>
            line.id === id ? { ...line, checked: Boolean(body.checked) } : line,
          );
          dashboard = {
            ...dashboard,
            planToday: { ...dashboard.planToday, lines },
          };
          const updated = lines.find((line) => line.id === id);
          return jsonResponse(updated);
        }
        if (url.includes("/plan/today/lines/") && method === "DELETE") {
          const id = url.split("/plan/today/lines/")[1];
          const lines = dashboard.planToday.lines.filter((line) => line.id !== id);
          dashboard = {
            ...dashboard,
            planToday: { ...dashboard.planToday, lines },
            status: { ...dashboard.status, ...quotaFor(dashboard.status.episodeLimit, lines.length) },
          };
          return { ok: true, status: 204, json: async () => ({}) };
        }
        return jsonResponse({}, 404);
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  it("adds, toggles, and removes PlanToday lines", async () => {
    const user = userEvent.setup();
    renderDashboard();

    expect(await screen.findByRole("heading", { name: "Plan today" })).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Pilot" })).not.toBeChecked();

    await user.type(screen.getByLabelText("What to watch today?"), "Episode 2");
    await user.click(screen.getByRole("button", { name: "Add to today" }));
    expect(await screen.findByRole("checkbox", { name: "Episode 2" })).toBeInTheDocument();

    const quota = screen.getByRole("heading", { name: "Screen time today" }).closest("section") as HTMLElement;
    const planned = () =>
      within(quota).getByText("Planned").parentElement?.querySelector(".value")?.textContent;
    expect(planned()).toBe("2");

    await user.click(screen.getByRole("checkbox", { name: "Pilot" }));
    await waitFor(() => {
      expect(screen.getByRole("checkbox", { name: "Pilot" })).toBeChecked();
    });
    expect(planned()).toBe("2");
    expect(screen.queryByText("Watched")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Remove Episode 2" }));
    await waitFor(() => {
      expect(screen.queryByRole("checkbox", { name: "Episode 2" })).not.toBeInTheDocument();
    });
    expect(screen.getByRole("checkbox", { name: "Pilot" })).toBeInTheDocument();
  });

  it("does not disable add when remaining is 0", async () => {
    dashboard = {
      ...dashboard,
      status: {
        ...dashboard.status,
        episodesPlanned: 2,
        episodesRemaining: 0,
        overQuota: false,
        canAddAnotherEpisode: false,
      },
      planToday: {
        date: "2026-07-27",
        lines: [
          { id: "line-1", contentTitle: "Pilot", checked: false, source: "MANUAL" },
          { id: "line-2", contentTitle: "Second", checked: false, source: "MANUAL" },
        ],
      },
    };
    renderDashboard();
    expect(await screen.findByRole("button", { name: "Add to today" })).not.toBeDisabled();
    expect(screen.getByText("Today's limit is reached.")).toBeInTheDocument();
  });

  it("invalidates dashboard and forward-plan queries after a PlanToday add", async () => {
    const user = userEvent.setup();
    const { queryClient } = renderDashboard();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    await screen.findByRole("heading", { name: "Plan today" });

    await user.type(screen.getByLabelText("What to watch today?"), "Late show");
    await user.click(screen.getByRole("button", { name: "Add to today" }));

    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] });
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["plan-forward"] });
    });
  });
});
