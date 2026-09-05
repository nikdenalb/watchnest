import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearCsrfCache } from "./api/http";
import { App } from "./App";
import { localDateIso } from "./archiveMonthRange";
import type { CurrentUser, WatchEvent } from "./types";

const alice: CurrentUser = { id: "user-a", username: "alice" };
const bob: CurrentUser = { id: "user-b", username: "bob" };

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

  return { ...render(<App />, { wrapper }), queryClient };
}

describe("App auth flow", () => {
  let session: CurrentUser | null;
  let eventsByOwner: Record<string, WatchEvent[]>;
  let fetchImpl: FetchImpl;
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    clearCsrfCache();
    session = null;
    eventsByOwner = {
      "user-a": [
        {
          id: "e1",
          ownerId: "user-a",
          watchedOn: localDateIso(),
          contentTitle: "Pilot",
        },
      ],
      "user-b": [],
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

      if (url.includes("/watch-events") && method === "GET") {
        if (!session) {
          return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
        }
        const parsed = new URL(url, "http://localhost");
        const from = parsed.searchParams.get("from") ?? "";
        const to = parsed.searchParams.get("to") ?? "";
        const events = (eventsByOwner[session.id] ?? []).filter(
          (event) => event.watchedOn >= from && event.watchedOn <= to,
        );
        return jsonResponse({ from, to, events });
      }

      return jsonResponse({}, 404);
    };

    fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => fetchImpl(input, init));
    vi.stubGlobal("fetch", fetchMock);
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

  function calledRemovedPlannerRoutes() {
    return fetchMock.mock.calls.some(([input]) => {
      const url = String(input);
      return (
        url.includes("/dashboard") ||
        url.includes("/plan/") ||
        url.includes("/policy") ||
        url.includes("/library-preferences")
      );
    });
  }

  it("shows auth UI after splash when unauthenticated", async () => {
    renderApp();
    await dismissSplash();

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("Failed to load library dashboard.")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Watch diary" })).not.toBeInTheDocument();
    expect(calledRemovedPlannerRoutes()).toBe(false);
  });

  it("lands on the diary after splash when already authenticated", async () => {
    session = alice;
    renderApp();
    await dismissSplash();

    expect(await screen.findByRole("heading", { name: "Watch diary" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "alice" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Log out" })).not.toBeInTheDocument();
    expect(await screen.findByText("Pilot")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Plan today" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Forward plan" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Screen time today" })).not.toBeInTheDocument();
    expect(screen.queryByText("Treat planned titles as watched")).not.toBeInTheDocument();
    expect(calledRemovedPlannerRoutes()).toBe(false);
  });

  it("registers a new user and loads their empty diary", async () => {
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await user.click(screen.getByRole("tab", { name: "Register" }));
    await user.type(screen.getByLabelText("Username"), "bob");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.type(screen.getByLabelText("Confirm password"), "password1");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("heading", { name: "Watch diary" })).toBeInTheDocument();
    expect(screen.getByText("bob")).toBeInTheDocument();
    expect(await screen.findByText("No watches this month.")).toBeInTheDocument();
    expect(calledRemovedPlannerRoutes()).toBe(false);
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

  it("logs out from the username menu and returns to auth UI", async () => {
    session = alice;
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();
    expect(await screen.findByText("Pilot")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "alice" }));
    await user.click(screen.getByRole("button", { name: "Log out" }));

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("Pilot")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Watch diary" })).not.toBeInTheDocument();
  });

  it("clears user-scoped state when watch-events returns 401", async () => {
    session = alice;
    renderApp();
    await dismissSplash();
    expect(await screen.findByText("Pilot")).toBeInTheDocument();

    session = null;
    fetchImpl = async (input) => {
      const url = String(input);
      if (url.includes("/auth/csrf")) {
        return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf-test" });
      }
      if (url.includes("/auth/me") || url.includes("/watch-events")) {
        return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
      }
      return jsonResponse({}, 404);
    };

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Previous month" }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    });
    expect(screen.queryByText("Pilot")).not.toBeInTheDocument();
  });

  it("never shows user A diary after switching to user B", async () => {
    const user = userEvent.setup();
    renderApp();
    await dismissSplash();

    await signIn("alice", "password1");
    expect(await screen.findByText("Pilot")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "alice" }));
    await user.click(screen.getByRole("button", { name: "Log out" }));
    await screen.findByRole("heading", { name: "Sign in" });

    await signIn("bob", "password1");
    expect(await screen.findByText("bob")).toBeInTheDocument();
    expect(screen.queryByText("Pilot")).not.toBeInTheDocument();
    expect(await screen.findByText("No watches this month.")).toBeInTheDocument();
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
    expect(screen.queryByRole("heading", { name: "Watch diary" })).not.toBeInTheDocument();
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
    expect(await screen.findByRole("heading", { name: "Watch diary" })).toBeInTheDocument();
  });
});
