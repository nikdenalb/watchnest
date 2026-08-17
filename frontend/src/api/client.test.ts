import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMe, login, logout, register } from "./auth";
import { clearCsrfCache, fetchCsrf } from "./http";
import {
  addForwardPlanItem,
  addPlanTodayLine,
  deleteForwardPlanItem,
  deletePlanTodayLine,
  fetchDashboard,
  fetchForwardPlan,
  fetchWatchEvents,
  patchPlanTodayLine,
  updatePolicy,
} from "./planner";
import type {
  Dashboard,
  ForwardPlan,
  ForwardPlanItem,
  PlanTodayLine,
  ScreenTimePolicy,
  WatchEventArchive,
} from "../types";

const dashboard: Dashboard = {
  displayName: "alice",
  today: "2026-07-27",
  status: {
    date: "2026-07-27",
    episodeLimit: 2,
    episodesPlanned: 0,
    episodesRemaining: 2,
    overQuota: false,
    canAddAnotherEpisode: true,
  },
  policy: {
    weekdayEpisodeLimit: 2,
    weekendEpisodeLimit: 4,
  },
  planToday: {
    date: "2026-07-27",
    lines: [],
  },
};

const csrf = { headerName: "X-XSRF-TOKEN", token: "csrf-1" };

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

describe("api client", () => {
  beforeEach(() => {
    clearCsrfCache();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  it("sends credentials on CSRF fetch", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(csrf));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchCsrf()).resolves.toEqual(csrf);
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/auth/csrf", {
      credentials: "include",
    });
  });

  it("sends credentials and CSRF header for register, login, logout, plan mutations, and policy", async () => {
    const line: PlanTodayLine = {
      id: "line-1",
      contentTitle: "Pilot",
      checked: false,
      source: "MANUAL",
    };
    const forwardItem: ForwardPlanItem = {
      id: "fwd-1",
      plannedFor: "2026-07-28",
      contentTitle: "Tomorrow",
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.includes("/auth/register")) {
        return jsonResponse({ id: "1", username: "alice" }, 201);
      }
      if (url.includes("/auth/login")) {
        return jsonResponse({ id: "1", username: "alice" });
      }
      if (url.includes("/auth/logout")) {
        return { ok: true, status: 204, json: async () => ({}) };
      }
      if (url.includes("/plan/today/lines/") && url.includes("line-1")) {
        return jsonResponse({ ...line, checked: true });
      }
      if (url.includes("/plan/today/lines")) {
        return jsonResponse(line, 201);
      }
      if (url.includes("/plan/forward/fwd-1")) {
        return { ok: true, status: 204, json: async () => ({}) };
      }
      if (url.includes("/plan/forward")) {
        return jsonResponse(forwardItem, 201);
      }
      if (url.includes("/policy")) {
        return jsonResponse({
          weekdayEpisodeLimit: 3,
          weekendEpisodeLimit: 5,
        } satisfies ScreenTimePolicy);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await register({ username: "alice", password: "password1" });
    await login({ username: "alice", password: "password1" });
    await logout();
    await addPlanTodayLine("Pilot");
    await patchPlanTodayLine("line-1", true);
    await deletePlanTodayLine("line-1");
    await addForwardPlanItem("2026-07-28", "Tomorrow");
    await deleteForwardPlanItem("fwd-1");
    await updatePolicy({ weekdayEpisodeLimit: 3, weekendEpisodeLimit: 5 });

    const unsafeCalls = fetchMock.mock.calls.filter(([input]) => {
      const url = String(input);
      return (
        url.includes("/register") ||
        url.includes("/login") ||
        url.includes("/logout") ||
        url.includes("/plan/") ||
        url.includes("/policy")
      );
    });

    expect(unsafeCalls.length).toBeGreaterThanOrEqual(8);
    for (const [, init] of unsafeCalls) {
      expect(init?.credentials).toBe("include");
      const headers = new Headers(init?.headers);
      expect(headers.get("X-XSRF-TOKEN")).toBeTruthy();
    }
  });

  it("preserves HTTP status and stable error code", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse({ code: "invalid_credentials", message: "Bad credentials" }, 401),
      ),
    );

    await expect(fetchDashboard()).rejects.toMatchObject({
      status: 401,
      code: "invalid_credentials",
      message: "Bad credentials",
    });
  });

  it("retries once after csrf_invalid", async () => {
    let policyAttempts = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse({
          headerName: "X-XSRF-TOKEN",
          token: `csrf-${fetchMock.mock.calls.filter(([u]) => String(u).includes("/csrf")).length}`,
        });
      }
      if (url.includes("/policy")) {
        policyAttempts += 1;
        if (policyAttempts === 1) {
          return jsonResponse({ code: "csrf_invalid", message: "CSRF" }, 403);
        }
        return jsonResponse({ weekdayEpisodeLimit: 2, weekendEpisodeLimit: 4 });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updatePolicy({ weekdayEpisodeLimit: 2, weekendEpisodeLimit: 4 }),
    ).resolves.toEqual({ weekdayEpisodeLimit: 2, weekendEpisodeLimit: 4 });
    expect(policyAttempts).toBe(2);
  });

  it("does not retry csrf_invalid more than once", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.includes("/policy")) {
        return jsonResponse({ code: "csrf_invalid", message: "CSRF" }, 403);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      updatePolicy({ weekdayEpisodeLimit: 2, weekendEpisodeLimit: 4 }),
    ).rejects.toMatchObject({ code: "csrf_invalid", status: 403 });

    const policyCalls = fetchMock.mock.calls.filter(([input]) => String(input).includes("/policy"));
    expect(policyCalls).toHaveLength(2);
  });

  it("uses GET with query params for archive and forward plan", async () => {
    const archive: WatchEventArchive = {
      from: "2026-07-01",
      to: "2026-07-27",
      events: [],
    };
    const forward: ForwardPlan = {
      from: "2026-07-27",
      to: "2026-08-02",
      items: [],
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/auth/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.includes("/watch-events") && method === "GET") {
        return jsonResponse(archive);
      }
      if (url.includes("/plan/forward") && method === "GET") {
        return jsonResponse(forward);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchWatchEvents("2026-07-01", "2026-07-27")).resolves.toEqual(archive);
    await expect(fetchForwardPlan("2026-07-27", "2026-08-02")).resolves.toEqual(forward);

    const archiveCall = fetchMock.mock.calls.find(([input]) => String(input).includes("/watch-events"));
    const forwardCall = fetchMock.mock.calls.find(([input]) => String(input).includes("/plan/forward"));

    expect(archiveCall?.[0]).toBe("/api/v1/watch-events?from=2026-07-01&to=2026-07-27");
    expect(archiveCall?.[1]).toEqual(
      expect.objectContaining({
        method: "GET",
        credentials: "include",
      }),
    );
    expect(new Headers(archiveCall?.[1]?.headers).get("X-XSRF-TOKEN")).toBeNull();
    expect(forwardCall?.[0]).toBe("/api/v1/plan/forward?from=2026-07-27&to=2026-08-02");
    expect(new Headers(forwardCall?.[1]?.headers).get("X-XSRF-TOKEN")).toBeNull();
  });

  it("fetches the dashboard with credentials", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(dashboard));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchDashboard()).resolves.toEqual(dashboard);
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/dashboard", {
      method: "GET",
      headers: expect.any(Headers),
      body: undefined,
      credentials: "include",
    });
  });

  it("maps /me 401 to null", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse({ code: "authentication_required", message: "Auth required" }, 401),
      ),
    );

    await expect(fetchMe()).resolves.toBeNull();
  });

  it("returns the current user from /me", async () => {
    const user = { id: "1", username: "alice" };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(user)));

    await expect(fetchMe()).resolves.toEqual(user);
  });

  it("keeps login success when post-auth CSRF refresh fails", async () => {
    let csrfCalls = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        csrfCalls += 1;
        if (csrfCalls === 1) {
          return jsonResponse(csrf);
        }
        return jsonResponse({ code: "request_failed", message: "csrf down" }, 500);
      }
      if (url.includes("/auth/login")) {
        return jsonResponse({ id: "1", username: "alice" });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(login({ username: "alice", password: "password1" })).resolves.toEqual({
      id: "1",
      username: "alice",
    });
  });
});
