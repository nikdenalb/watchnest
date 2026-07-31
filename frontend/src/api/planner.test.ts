import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchDashboard, logWatchEvent, updatePolicy } from "./planner";
import type { Dashboard, ScreenTimePolicy, WatchEvent } from "../types";

const dashboard: Dashboard = {
  displayName: "You",
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

describe("planner api", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("fetches the dashboard", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => dashboard,
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchDashboard()).resolves.toEqual(dashboard);
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/dashboard");
  });

  it("logs a watch event", async () => {
    const event: WatchEvent = {
      id: "1",
      ownerId: "owner",
      watchedOn: "2026-07-27",
      contentTitle: "Pilot",
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => event,
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(logWatchEvent("Pilot")).resolves.toEqual(event);
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/watch-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ contentTitle: "Pilot" }),
    });
  });

  it("updates policy", async () => {
    const policy: ScreenTimePolicy = {
      weekdayEpisodeLimit: 3,
      weekendEpisodeLimit: 5,
    };
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => policy,
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(updatePolicy(policy)).resolves.toEqual(policy);
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/policy", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(policy),
    });
  });

  it("throws when the response is not ok", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
      }),
    );

    await expect(fetchDashboard()).rejects.toThrow("Request failed with status 500");
  });
});
