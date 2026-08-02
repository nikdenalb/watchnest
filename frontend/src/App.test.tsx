import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearCsrfCache } from "./api/http";
import { App } from "./App";
import type { CurrentUser, Dashboard } from "./types";

const alice: CurrentUser = { id: "user-a", username: "alice" };
const bob: CurrentUser = { id: "user-b", username: "bob" };

const aliceDashboard: Dashboard = {
  displayName: "alice",
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
      ownerId: "user-a",
      watchedOn: "2026-07-27",
      contentTitle: "Pilot",
    },
  ],
};

const bobDashboard: Dashboard = {
  displayName: "bob",
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

type FetchImpl = (input: RequestInfo | URL, init?: RequestInit) => Promise<{
  ok: boolean;
  status: number;
  json: () => Promise<unknown>;
}>;

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

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

describe("App auth flow", () => {
  let session: CurrentUser | null;
  let dashboards: Record<string, Dashboard>;
  let fetchImpl: FetchImpl;

  beforeEach(() => {
    clearCsrfCache();
    session = null;
    dashboards = {
      "user-a": structuredClone(aliceDashboard),
      "user-b": structuredClone(bobDashboard),
    };

    fetchImpl = async (input, init) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();

      if (url.includes("/auth/csrf")) {
        return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
      }

      if (url.includes("/auth/me")) {
        if (!session) {
          return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
        }
        return jsonResponse(session);
      }

      if (url.includes("/auth/register") && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as { username?: string };
        if (body.username?.toLowerCase() === "alice") {
          return jsonResponse({ code: "username_already_exists", message: "Taken" }, 409);
        }
        session = { id: "user-b", username: (body.username ?? "bob").toLowerCase() };
        return jsonResponse(session, 201);
      }

      if (url.includes("/auth/login") && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as {
          username?: string;
          password?: string;
        };
        if (body.password !== "password1") {
          return jsonResponse({ code: "invalid_credentials", message: "Bad" }, 401);
        }
        if (body.username?.toLowerCase() === "alice") {
          session = alice;
          return jsonResponse(session);
        }
        if (body.username?.toLowerCase() === "bob") {
          session = bob;
          return jsonResponse(session);
        }
        return jsonResponse({ code: "invalid_credentials", message: "Bad" }, 401);
      }

      if (url.includes("/auth/logout") && method === "POST") {
        session = null;
        return { ok: true, status: 204, json: async () => ({}) };
      }

      if (url.includes("/dashboard")) {
        if (!session) {
          return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
        }
        return jsonResponse(dashboards[session.id]);
      }

      if (url.includes("/watch-events") && method === "POST") {
        if (!session) {
          return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
        }
        const body = JSON.parse(String(init?.body ?? "{}")) as { contentTitle?: string };
        const event = {
          id: "evt-new",
          ownerId: session.id,
          watchedOn: "2026-07-27",
          contentTitle: body.contentTitle ?? "",
        };
        const current = dashboards[session.id];
        dashboards[session.id] = {
          ...current,
          todayEvents: [...current.todayEvents, event],
          status: {
            ...current.status,
            episodesWatched: current.status.episodesWatched + 1,
            episodesRemaining: Math.max(0, current.status.episodesRemaining - 1),
            canWatchAnotherEpisode: current.status.episodesRemaining - 1 > 0,
          },
        };
        return jsonResponse(event);
      }

      if (url.includes("/policy") && method === "PUT") {
        if (!session) {
          return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
        }
        const body = JSON.parse(String(init?.body ?? "{}")) as {
          weekdayEpisodeLimit: number;
          weekendEpisodeLimit: number;
        };
        const current = dashboards[session.id];
        dashboards[session.id] = {
          ...current,
          policy: body,
          status: {
            ...current.status,
            episodeLimit: body.weekdayEpisodeLimit,
            episodesRemaining: body.weekdayEpisodeLimit - current.status.episodesWatched,
          },
        };
        return jsonResponse(body);
      }

      return jsonResponse({}, 404);
    };

    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL, init?: RequestInit) => fetchImpl(input, init)),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  async function dismissSplash() {
    const user = userEvent.setup();
    await screen.findByLabelText(/WatchNest splash screen/i);
    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: /Press any key or click to continue/i }),
      ).toBeInTheDocument();
    });
    await user.keyboard("{Enter}");
  }

  async function signIn(username: string, password: string) {
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Username"), username);
    await user.type(screen.getByLabelText("Password"), password);
    await user.click(screen.getByRole("button", { name: "Sign in" }));
  }

  it("shows auth UI after splash when unauthenticated, not a dashboard failure", async () => {
    renderApp();
    await dismissSplash();

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("Failed to load library dashboard.")).not.toBeInTheDocument();
    expect(screen.queryByText("Your watch day")).not.toBeInTheDocument();
  });

  it("shows the dashboard after splash when already authenticated", async () => {
    session = alice;
    renderApp();
    await dismissSplash();

    expect(await screen.findByRole("heading", { name: "Your watch day" })).toBeInTheDocument();
    expect(screen.getByText("alice")).toBeInTheDocument();
    expect(screen.getByText("Pilot")).toBeInTheDocument();
  });

  it("registers a new user and loads their empty dashboard", async () => {
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await user.click(screen.getByRole("tab", { name: "Register" }));
    await user.type(screen.getByLabelText("Username"), "bob");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.type(screen.getByLabelText("Confirm password"), "password1");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("heading", { name: "Your watch day" })).toBeInTheDocument();
    expect(screen.getByText("bob")).toBeInTheDocument();
    expect(screen.getByText("No watches logged yet today.")).toBeInTheDocument();
  });

  it("shows a generic message for invalid login", async () => {
    renderApp();
    await dismissSplash();
    await signIn("alice", "wrong-password");

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password.");
    expect(screen.getByLabelText("Password")).toHaveValue("");
  });

  it("shows a conflict message for duplicate registration", async () => {
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await user.click(screen.getByRole("tab", { name: "Register" }));
    await user.type(screen.getByLabelText("Username"), "alice");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.type(screen.getByLabelText("Confirm password"), "password1");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("That username is already taken.");
  });

  it("logs out, clears the dashboard, and returns to auth UI", async () => {
    session = alice;
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();
    await screen.findByText("Pilot");

    await user.click(screen.getByRole("button", { name: "Log out" }));

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("Pilot")).not.toBeInTheDocument();
    expect(screen.queryByText("Your watch day")).not.toBeInTheDocument();
  });

  it("clears prior-user dashboard data when planner returns 401", async () => {
    session = alice;
    renderApp();
    await dismissSplash();
    await screen.findByText("Pilot");

    session = null;
    fetchImpl = async (input) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
      }
      if (url.includes("/auth/me") || url.includes("/dashboard") || url.includes("/watch-events")) {
        return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
      }
      return jsonResponse({}, 404);
    };

    const user = userEvent.setup();
    await user.type(screen.getByLabelText("What was watched?"), "Late show");
    await user.click(screen.getByRole("button", { name: "Add to watch log" }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    });
    expect(screen.queryByText("Pilot")).not.toBeInTheDocument();
  });

  it("never shows user A dashboard after switching to user B", async () => {
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await signIn("alice", "password1");
    await screen.findByText("Pilot");
    await user.click(screen.getByRole("button", { name: "Log out" }));
    await screen.findByRole("heading", { name: "Sign in" });

    await signIn("bob", "password1");
    expect(await screen.findByText("bob")).toBeInTheDocument();
    expect(screen.queryByText("Pilot")).not.toBeInTheDocument();
    expect(screen.getByText("No watches logged yet today.")).toBeInTheDocument();
  });

  it("keeps mismatched register passwords on the client", async () => {
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await user.click(screen.getByRole("tab", { name: "Register" }));
    await user.type(screen.getByLabelText("Username"), "carol");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.type(screen.getByLabelText("Confirm password"), "password2");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Passwords do not match.");
    expect(screen.queryByText("Your watch day")).not.toBeInTheDocument();
  });

  it("keeps authenticated watch and policy flows working", async () => {
    session = alice;
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();
    await screen.findByRole("heading", { name: "Your watch day" });

    await user.type(screen.getByLabelText("What was watched?"), "Episode 2");
    await user.click(screen.getByRole("button", { name: "Add to watch log" }));
    await waitFor(() => {
      expect(screen.getByText("Episode 2")).toBeInTheDocument();
    });

    const weekday = screen.getByLabelText("Weekday limit");
    const weekend = screen.getByLabelText("Weekend limit");
    await user.clear(weekday);
    await user.type(weekday, "3");
    await user.clear(weekend);
    await user.type(weekend, "5");
    await user.click(screen.getByRole("button", { name: "Save rules" }));

    await waitFor(() => {
      const quota = screen.getByRole("heading", { name: "Screen time today" }).closest("section");
      expect(quota).not.toBeNull();
      expect(within(quota as HTMLElement).getByText("3")).toBeInTheDocument();
    });
  });

  it("resolves splash when unauthenticated", async () => {
    renderApp();
    await dismissSplash();
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });

  it("resolves splash when authenticated", async () => {
    session = alice;
    renderApp();
    await dismissSplash();
    expect(await screen.findByRole("heading", { name: "Your watch day" })).toBeInTheDocument();
  });
});
