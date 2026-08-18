import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearCsrfCache } from "./api/http";
import { ForwardPlanSection, groupForwardItemsByDate } from "./ForwardPlanSection";
import type { ForwardPlanItem } from "./types";

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

function renderForward(today = "2026-07-27", queryClient?: QueryClient) {
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
  return { ...render(<ForwardPlanSection today={today} />, { wrapper }), queryClient: client };
}

describe("groupForwardItemsByDate", () => {
  it("groups consecutive same-day titles and keeps date order", () => {
    const grouped = groupForwardItemsByDate([
      { id: "1", plannedFor: "2026-07-28", contentTitle: "Tue A" },
      { id: "2", plannedFor: "2026-07-28", contentTitle: "Tue B" },
      { id: "3", plannedFor: "2026-07-30", contentTitle: "Thu" },
    ]);
    expect(grouped).toEqual([
      {
        plannedFor: "2026-07-28",
        items: [
          { id: "1", plannedFor: "2026-07-28", contentTitle: "Tue A" },
          { id: "2", plannedFor: "2026-07-28", contentTitle: "Tue B" },
        ],
      },
      {
        plannedFor: "2026-07-30",
        items: [{ id: "3", plannedFor: "2026-07-30", contentTitle: "Thu" }],
      },
    ]);
  });
});

describe("ForwardPlanSection", () => {
  let items: ForwardPlanItem[];
  let todayPosts: string[];
  let forwardPosts: { plannedFor: string; contentTitle: string }[];
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    clearCsrfCache();
    items = [{ id: "fwd-1", plannedFor: "2026-07-28", contentTitle: "Tuesday film" }];
    todayPosts = [];
    forwardPosts = [];

    fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();

      if (url.includes("/auth/csrf")) {
        return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
      }
      if (url.includes("/plan/forward") && method === "GET") {
        const parsed = new URL(url, "http://localhost");
        const from = parsed.searchParams.get("from") ?? "";
        const to = parsed.searchParams.get("to") ?? "";
        return jsonResponse({
          from,
          to,
          items: items.filter((item) => item.plannedFor >= from && item.plannedFor <= to),
        });
      }
      if (url.includes("/plan/today/lines") && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as { contentTitle?: string };
        todayPosts.push(body.contentTitle ?? "");
        return jsonResponse(
          { id: "line-new", contentTitle: body.contentTitle, checked: false, source: "MANUAL" },
          201,
        );
      }
      if (url.includes("/plan/forward") && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as {
          plannedFor?: string;
          contentTitle?: string;
        };
        const item: ForwardPlanItem = {
          id: `fwd-${items.length + 1}`,
          plannedFor: body.plannedFor ?? "",
          contentTitle: body.contentTitle ?? "",
        };
        items = [...items, item];
        forwardPosts.push({ plannedFor: item.plannedFor, contentTitle: item.contentTitle });
        return jsonResponse(item, 201);
      }
      if (url.includes("/plan/forward/") && method === "DELETE") {
        const id = url.split("/plan/forward/")[1];
        items = items.filter((item) => item.id !== id);
        return { ok: true, status: 204, json: async () => ({}) };
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  it("loads the ISO week range and lists items by date, not as a calendar grid", async () => {
    renderForward();
    expect(await screen.findByText("Tuesday film")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "28 Jul" })).toBeInTheDocument();
    expect(screen.queryByRole("grid")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Week" })).toHaveAttribute("aria-pressed", "true");

    const weekGet = fetchMock.mock.calls.find(
      ([input, init]) =>
        String(input).includes("/plan/forward?") && (init?.method ?? "GET").toUpperCase() === "GET",
    );
    expect(weekGet?.[0]).toBe("/api/v1/plan/forward?from=2026-07-27&to=2026-08-02");
  });

  it("requests month and year display ranges over the same collection", async () => {
    const user = userEvent.setup();
    renderForward();
    await screen.findByText("Tuesday film");

    await user.click(screen.getByRole("button", { name: "Month" }));
    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([input]) =>
          String(input).includes("/api/v1/plan/forward?from=2026-07-01&to=2026-07-31"),
        ),
      ).toBe(true);
    });
    expect(screen.getByText("July 2026")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Year" }));
    await waitFor(() => {
      expect(
        fetchMock.mock.calls.some(([input]) =>
          String(input).includes("/api/v1/plan/forward?from=2026-01-01&to=2026-12-31"),
        ),
      ).toBe(true);
    });
    expect(screen.getByText("2026")).toBeInTheDocument();
  });

  it("adds a future title through forward POST and removes it", async () => {
    const user = userEvent.setup();
    renderForward();
    await screen.findByText("Tuesday film");

    fireEvent.change(screen.getByLabelText("Plan for"), { target: { value: "2026-07-30" } });
    await user.type(screen.getByLabelText("Forward title"), "Thursday episode");
    await user.click(screen.getByRole("button", { name: "Add to plan" }));

    expect(await screen.findByText("Thursday episode")).toBeInTheDocument();
    expect(forwardPosts).toEqual([{ plannedFor: "2026-07-30", contentTitle: "Thursday episode" }]);
    expect(todayPosts).toEqual([]);

    await user.click(screen.getByRole("button", { name: "Remove Tuesday film" }));
    await waitFor(() => {
      expect(screen.queryByText("Tuesday film")).not.toBeInTheDocument();
    });
  });

  it("defaults min and value to tomorrow and never posts PlanToday", async () => {
    items = [
      { id: "fwd-today", plannedFor: "2026-07-27", contentTitle: "Stuck today" },
      { id: "fwd-1", plannedFor: "2026-07-28", contentTitle: "Tuesday film" },
    ];
    const user = userEvent.setup();
    renderForward();
    expect(await screen.findByText("Tuesday film")).toBeInTheDocument();
    expect(screen.getByText("Stuck today")).toBeInTheDocument();

    const dateInput = screen.getByLabelText("Plan for");
    expect(dateInput).toHaveAttribute("min", "2026-07-28");
    expect(dateInput).toHaveValue("2026-07-28");
    expect(screen.queryByRole("button", { name: "Remove Stuck today" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Remove Tuesday film" })).toBeInTheDocument();

    fireEvent.change(dateInput, { target: { value: "2026-07-27" } });
    await user.type(screen.getByLabelText("Forward title"), "Same day");
    await user.click(screen.getByRole("button", { name: "Add to plan" }));

    expect(todayPosts).toEqual([]);
    expect(forwardPosts).toEqual([]);
    expect(
      fetchMock.mock.calls.some(
        ([input, init]) =>
          String(input).includes("/plan/today/lines") && (init?.method ?? "").toUpperCase() === "POST",
      ),
    ).toBe(false);
  });

  it("does not POST when the selected date is in the past", async () => {
    const user = userEvent.setup();
    renderForward();
    await screen.findByText("Tuesday film");

    const dateInput = screen.getByLabelText("Plan for") as HTMLInputElement;
    dateInput.removeAttribute("min");
    fireEvent.change(dateInput, { target: { value: "2026-07-26" } });
    await user.type(screen.getByLabelText("Forward title"), "Missed");
    await user.click(screen.getByRole("button", { name: "Add to plan" }));

    expect(todayPosts).toEqual([]);
    expect(forwardPosts).toEqual([]);
  });
});
