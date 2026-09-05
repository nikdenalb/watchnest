import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMe, login, logout, register } from "./auth";
import { clearCsrfCache, fetchCsrf } from "./http";
import { addWatchEvent, deleteWatchEvent, fetchWatchEvents, patchWatchEvent } from "./planner";
import type { WatchEvent, WatchEventRange } from "../types";

const csrf = { headerName: "X-XSRF-TOKEN", token: "csrf-1" };

const event: WatchEvent = {
  id: "e1",
  ownerId: "1",
  watchedOn: "2026-07-26",
  contentTitle: "Pilot",
};

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
      cache: "no-store",
    });
  });

  it("sends credentials and CSRF header for register, login, logout, and watch-event mutations", async () => {
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
      if (url.includes("/watch-events/e1")) {
        return jsonResponse(event);
      }
      if (url.includes("/watch-events")) {
        return jsonResponse(event, 201);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await register({ username: "alice", password: "password1" });
    await login({ username: "alice", password: "password1" });
    await logout();
    await addWatchEvent("2026-07-26", "Pilot");
    await patchWatchEvent("e1", "Pilot");
    await deleteWatchEvent("e1");

    const unsafeCalls = fetchMock.mock.calls.filter(([input]) => {
      const url = String(input);
      return (
        url.includes("/register") ||
        url.includes("/login") ||
        url.includes("/logout") ||
        url.includes("/watch-events")
      );
    });

    expect(unsafeCalls.length).toBeGreaterThanOrEqual(6);
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

    await expect(addWatchEvent("2026-07-26", "Pilot")).rejects.toMatchObject({
      status: 401,
      code: "invalid_credentials",
      message: "Bad credentials",
    });
  });

  it("retries once after csrf_invalid", async () => {
    let addAttempts = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse({
          headerName: "X-XSRF-TOKEN",
          token: `csrf-${fetchMock.mock.calls.filter(([u]) => String(u).includes("/csrf")).length}`,
        });
      }
      if (url.includes("/watch-events") && !url.includes("/watch-events/")) {
        addAttempts += 1;
        if (addAttempts === 1) {
          return jsonResponse({ code: "csrf_invalid", message: "CSRF" }, 403);
        }
        return jsonResponse(event, 201);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(addWatchEvent("2026-07-26", "Pilot")).resolves.toEqual(event);
    expect(addAttempts).toBe(2);
  });

  it("does not retry csrf_invalid more than once", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.includes("/watch-events")) {
        return jsonResponse({ code: "csrf_invalid", message: "CSRF" }, 403);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(addWatchEvent("2026-07-26", "Pilot")).rejects.toMatchObject({
      code: "csrf_invalid",
      status: 403,
    });

    const eventCalls = fetchMock.mock.calls.filter(([input]) => String(input).includes("/watch-events"));
    expect(eventCalls).toHaveLength(2);
  });

  it("uses GET with query params for watch-event ranges and omits CSRF", async () => {
    const range: WatchEventRange = {
      from: "2026-07-01",
      to: "2026-07-31",
      events: [],
    };
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/auth/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.includes("/watch-events") && method === "GET") {
        return jsonResponse(range);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchWatchEvents("2026-07-01", "2026-07-31")).resolves.toEqual(range);

    const rangeCall = fetchMock.mock.calls.find(([input]) => String(input).includes("/watch-events"));
    expect(rangeCall?.[0]).toBe("/api/v1/watch-events?from=2026-07-01&to=2026-07-31");
    expect(rangeCall?.[1]).toEqual(
      expect.objectContaining({
        method: "GET",
        credentials: "include",
      }),
    );
    expect(new Headers(rangeCall?.[1]?.headers).get("X-XSRF-TOKEN")).toBeNull();
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
