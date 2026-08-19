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
  treatPlanAsWatched: false,
};

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

type FetchImpl = (input: RequestInfo | URL, init?: RequestInit) => Promise<{
  ok: boolean;
  status: number;
  json: () => Promise<unknown>;
}>;

function quotaFor(limit: number, planned: number) {
  return {
    episodesPlanned: planned,
    episodesRemaining: Math.max(0, limit - planned),
    overQuota: planned > limit,
    canAddAnotherEpisode: planned < limit,
  };
}

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function renderDashboard(queryClient?: QueryClient) {
  const client = queryClient ?? createQueryClient();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { ...render(<Dashboard user={alice} />, { wrapper }), queryClient: client };
}

function planTodayCard() {
  return screen.getByRole("heading", { name: "Plan today" }).closest("article") as HTMLElement;
}

function accountCard() {
  return screen.getByRole("heading", { name: "Account" }).closest("section") as HTMLElement;
}

function isPatchPlanTodayLine(url: string, init?: RequestInit) {
  return url.includes("/plan/today/lines/") && (init?.method ?? "GET").toUpperCase() === "PATCH";
}

describe("PlanTodaySection", () => {
  let dashboard: DashboardData;
  let events: WatchEvent[];
  let forwardItems: ForwardPlanItem[];
  let defaultFetch: FetchImpl;
  let fetchMock: ReturnType<typeof vi.fn<FetchImpl>>;

  beforeEach(() => {
    clearCsrfCache();
    dashboard = structuredClone(aliceDashboard);
    events = [];
    forwardItems = [];

    defaultFetch = async (input, init) => {
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
      if (url.includes("/library-preferences") && method === "PUT") {
        const body = JSON.parse(String(init?.body ?? "{}")) as { treatPlanAsWatched?: boolean };
        const treatPlanAsWatched = Boolean(body.treatPlanAsWatched);
        dashboard = {
          ...dashboard,
          treatPlanAsWatched,
          planToday: {
            ...dashboard.planToday,
            lines: treatPlanAsWatched
              ? dashboard.planToday.lines.map((line) => ({ ...line, checked: true }))
              : dashboard.planToday.lines,
          },
        };
        return jsonResponse({ treatPlanAsWatched });
      }
      if (url.includes("/plan/today/lines") && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as { contentTitle?: string };
        const line: PlanTodayLine = {
          id: `line-${dashboard.planToday.lines.length + 1}`,
          contentTitle: body.contentTitle ?? "",
          checked: dashboard.treatPlanAsWatched,
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
    };
    fetchMock = vi.fn(defaultFetch);
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  it("adds, toggles, and removes PlanToday lines when the flag is off", async () => {
    const user = userEvent.setup();
    renderDashboard();

    expect(await screen.findByRole("heading", { name: "Plan today" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Account" })).toBeInTheDocument();
    expect(screen.getByText("Checked titles move to watch history when the day rolls.")).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Pilot" })).not.toBeChecked();
    expect(planTodayCard().querySelector(".plan-line-check")).not.toBeNull();

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

  it("hides PlanToday checkboxes when the flag is on and never PATCHes checked", async () => {
    const user = userEvent.setup();
    dashboard = { ...dashboard, treatPlanAsWatched: true };
    renderDashboard();

    expect(await screen.findByRole("heading", { name: "Plan today" })).toBeInTheDocument();
    expect(
      screen.getByText(
        "Titles left here are added to watch history when WatchNest moves to a new day. Remove anything you did not watch.",
      ),
    ).toBeInTheDocument();
    expect(within(planTodayCard()).queryByRole("checkbox")).not.toBeInTheDocument();
    expect(planTodayCard().querySelector(".plan-line-check")).toBeNull();
    expect(within(planTodayCard()).getByText("Pilot")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Pilot" })).toBeInTheDocument();

    await user.click(within(planTodayCard()).getByText("Pilot"));
    expect(fetchMock.mock.calls.some(([input, init]) => isPatchPlanTodayLine(String(input), init))).toBe(
      false,
    );

    await user.type(screen.getByLabelText("What to watch today?"), "Episode 2");
    await user.click(screen.getByRole("button", { name: "Add to today" }));
    expect(await within(planTodayCard()).findByText("Episode 2")).toBeInTheDocument();
    expect(within(planTodayCard()).queryByRole("checkbox")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Episode 2" })).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([input, init]) => isPatchPlanTodayLine(String(input), init))).toBe(
      false,
    );
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

  it("PUTs library preferences and invalidates dashboard, forward, and archive roots", async () => {
    const user = userEvent.setup();
    const { queryClient } = renderDashboard();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    const preference = await screen.findByRole("checkbox", { name: "Treat planned titles as watched" });
    expect(preference).not.toBeChecked();

    await user.click(preference);

    await waitFor(() => {
      const put = fetchMock.mock.calls.find(
        ([input, init]) =>
          String(input).includes("/library-preferences") && (init?.method ?? "").toUpperCase() === "PUT",
      );
      expect(put).toBeTruthy();
      expect(JSON.parse(String(put?.[1]?.body))).toEqual({ treatPlanAsWatched: true });
    });
    await waitFor(() => {
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] });
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["plan-forward"] });
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["watch-events"] });
    });
    await waitFor(() => {
      expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).toBeChecked();
    });
  });

  it("disables the Account control while the preference PUT is pending", async () => {
    const user = userEvent.setup();
    let release!: () => void;
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });
    fetchMock.mockImplementation(async (input, init) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/library-preferences") && method === "PUT") {
        await held;
        dashboard = { ...dashboard, treatPlanAsWatched: true };
        return jsonResponse({ treatPlanAsWatched: true });
      }
      return defaultFetch(input, init);
    });

    renderDashboard();
    const preference = await screen.findByRole("checkbox", { name: "Treat planned titles as watched" });
    await user.click(preference);

    await waitFor(() => {
      expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).toBeDisabled();
    });
    expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).not.toBeChecked();

    release();
    await waitFor(() => {
      expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).toBeEnabled();
      expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).toBeChecked();
    });
  });

  it("keeps a non-401 preference error in the Account card", async () => {
    const user = userEvent.setup();
    fetchMock.mockImplementation(async (input, init) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/library-preferences") && method === "PUT") {
        return jsonResponse({ code: "validation_failed", message: "Could not save preference." }, 400);
      }
      return defaultFetch(input, init);
    });

    renderDashboard();
    await user.click(await screen.findByRole("checkbox", { name: "Treat planned titles as watched" }));

    expect(await within(accountCard()).findByText("Could not save preference.")).toBeInTheDocument();
    expect(within(accountCard()).getByText("Could not save preference.").tagName).toBe("P");
    expect(within(accountCard()).getByText("Could not save preference.")).toHaveClass("status-note");
    expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Treat planned titles as watched" })).toBeEnabled();
  });

  it("clears user-scoped queries when the preference PUT returns 401", async () => {
    const user = userEvent.setup();
    const queryClient = createQueryClient();
    queryClient.setQueryData(["me"], alice);
    fetchMock.mockImplementation(async (input, init) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/library-preferences") && method === "PUT") {
        return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
      }
      return defaultFetch(input, init);
    });

    renderDashboard(queryClient);
    await user.click(await screen.findByRole("checkbox", { name: "Treat planned titles as watched" }));

    await waitFor(() => {
      expect(queryClient.getQueryData(["me"])).toBeNull();
    });
  });
});
