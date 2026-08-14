import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMe, login, logout, register } from "./auth";
import { clearCsrfCache, fetchCsrf } from "./http";
import { fetchDashboard, fetchWatchEvents, logWatchEvent, updatePolicy } from "./planner";
import type { Dashboard, ScreenTimePolicy, WatchEvent, WatchEventArchive } from "../types";

const dashboard: Dashboard = {
  displayName: "alice",
  today: "2026-07-27",
  status: {
    date: "2026-07-27",
    episodeLimit: 2,
    episodesWatched: 0,
    episodesRemaining: 2,
    overQuota: false,
    canWatchAnotherEpisode: true,
  },
  policy: {
    weekdayEpisodeLimit: 2,
    weekendEpisodeLimit: 4,
  },
  todayEvents: [],
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

  it("sends credentials and CSRF header for register, login, logout, watch, and policy", async () => {
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
      if (url.includes("/watch-events")) {
        return jsonResponse({
          id: "e1",
          ownerId: "1",
          watchedOn: "2026-07-27",
          contentTitle: "Pilot",
        } satisfies WatchEvent);
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
    await logWatchEvent("Pilot");
    await updatePolicy({ weekdayEpisodeLimit: 3, weekendEpisodeLimit: 5 });

    const unsafeCalls = fetchMock.mock.calls.filter(([input]) => {
      const url = String(input);
      return (
        url.includes("/register") ||
        url.includes("/login") ||
        url.includes("/logout") ||
        url.includes("/watch-events") ||
        url.includes("/policy")
      );
    });

    expect(unsafeCalls.length).toBeGreaterThanOrEqual(5);
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

  it("uses GET with query params for archive and POST with CSRF for logging", async () => {
    const archive: WatchEventArchive = {
      from: "2026-07-01",
      to: "2026-07-27",
      events: [],
    };
    const event: WatchEvent = {
      id: "e1",
      ownerId: "1",
      watchedOn: "2026-07-27",
      contentTitle: "Pilot",
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
      if (url.includes("/watch-events") && method === "POST") {
        return jsonResponse(event, 201);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchWatchEvents("2026-07-01", "2026-07-27")).resolves.toEqual(archive);
    await expect(logWatchEvent("Pilot")).resolves.toEqual(event);

    const getCall = fetchMock.mock.calls.find(
      ([input, init]) =>
        String(input).includes("/watch-events") && (init?.method ?? "GET").toUpperCase() === "GET",
    );
    const postCall = fetchMock.mock.calls.find(
      ([input, init]) =>
        String(input).includes("/watch-events") && (init?.method ?? "GET").toUpperCase() === "POST",
    );

    expect(getCall?.[0]).toBe("/api/v1/watch-events?from=2026-07-01&to=2026-07-27");
    expect(getCall?.[1]).toEqual(
      expect.objectContaining({
        method: "GET",
        credentials: "include",
      }),
    );
    expect(new Headers(getCall?.[1]?.headers).get("X-XSRF-TOKEN")).toBeNull();

    expect(postCall?.[1]).toEqual(
      expect.objectContaining({
        method: "POST",
        credentials: "include",
      }),
    );
    expect(new Headers(postCall?.[1]?.headers).get("X-XSRF-TOKEN")).toBe(csrf.token);
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
