import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMe, login, logout } from "./auth";
import { clearCsrfCache, fetchCsrf } from "./http";
import { createTitle, deleteTitle, fetchTitle, fetchTitles, updateTitle } from "./titles";
import type { CatalogTitle, TitleWrite } from "../types";

const csrf = { headerName: "X-WATCHNEST-CMS-XSRF-TOKEN", token: "cms-csrf-1" };

const dune: CatalogTitle = {
  id: "00000000-0000-0000-0000-000000000000",
  type: "FILM",
  nameEn: "Dune",
  nameOriginal: "Dune",
  year: 2021,
  description: null,
  genres: "drama, science fiction",
  countries: "united states, canada",
};

const write: TitleWrite = {
  type: "FILM",
  nameEn: "Dune",
  nameOriginal: "Dune",
  year: 2021,
  description: null,
  genres: "Drama, Science Fiction",
  countries: "United States, Canada",
};

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

function assertCmsApiOnly(fetchMock: ReturnType<typeof vi.fn>) {
  expect(fetchMock.mock.calls.length).toBeGreaterThan(0);
  for (const [input] of fetchMock.mock.calls) {
    const url = String(input);
    expect(url.startsWith("/cms/api/v1")).toBe(true);
    expect(url.includes("/api/v1/auth")).toBe(false);
  }
}

function assertCsrfImmediatelyBeforeEachUnsafe(fetchMock: ReturnType<typeof vi.fn>) {
  const calls = fetchMock.mock.calls;
  for (let index = 0; index < calls.length; index += 1) {
    const [input, init] = calls[index];
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    if (["GET", "HEAD", "OPTIONS", "TRACE"].includes(method) || url.includes("/csrf")) {
      continue;
    }
    expect(index).toBeGreaterThan(0);
    const [prevInput, prevInit] = calls[index - 1];
    expect(String(prevInput)).toBe("/cms/api/v1/csrf");
    expect(prevInit?.credentials).toBe("include");
    expect(prevInit?.cache).toBe("no-store");
  }
}

describe("cms api client", () => {
  beforeEach(() => {
    clearCsrfCache();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    clearCsrfCache();
  });

  it("sends credentials on CMS CSRF fetch", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(csrf));
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchCsrf()).resolves.toEqual(csrf);
    expect(fetchMock).toHaveBeenCalledWith("/cms/api/v1/csrf", {
      credentials: "include",
      cache: "no-store",
    });
    assertCmsApiOnly(fetchMock);
  });

  it("sends credentials and CMS CSRF header for login, logout, and title writes", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.endsWith("/login")) {
        return jsonResponse({ id: "1", username: "editor" });
      }
      if (url.endsWith("/logout")) {
        return { ok: true, status: 204, json: async () => ({}) };
      }
      if (url.endsWith("/titles") && !url.includes("?")) {
        return jsonResponse(dune, 201);
      }
      if (url.includes("/titles/")) {
        if (url.endsWith("/titles/title-1")) {
          return { ok: true, status: 204, json: async () => ({}) };
        }
        return jsonResponse(dune);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await login({ username: "editor", password: "password1" });
    await logout();
    await createTitle(write);
    await updateTitle(dune.id, write);
    await deleteTitle("title-1");

    const unsafeCalls = fetchMock.mock.calls.filter(([input]) => {
      const url = String(input);
      return (
        url.endsWith("/login") ||
        url.endsWith("/logout") ||
        url.includes("/titles")
      ) && !url.includes("/csrf");
    });

    expect(unsafeCalls.length).toBeGreaterThanOrEqual(5);
    for (const [, init] of unsafeCalls) {
      expect(init?.credentials).toBe("include");
      const method = (init?.method ?? "GET").toUpperCase();
      if (method !== "GET") {
        const headers = new Headers(init?.headers);
        expect(headers.get("X-WATCHNEST-CMS-XSRF-TOKEN")).toBeTruthy();
        expect(headers.get("X-XSRF-TOKEN")).toBeNull();
      }
    }
    assertCsrfImmediatelyBeforeEachUnsafe(fetchMock);
    assertCmsApiOnly(fetchMock);
  });

  it("fetches CSRF immediately before login, logout, and title writes", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        return jsonResponse({
          headerName: "X-WATCHNEST-CMS-XSRF-TOKEN",
          token: `csrf-${fetchMock.mock.calls.filter(([u]) => String(u).includes("/csrf")).length}`,
        });
      }
      if (url.endsWith("/login")) {
        return jsonResponse({ id: "1", username: "editor" });
      }
      if (url.endsWith("/logout")) {
        return { ok: true, status: 204, json: async () => ({}) };
      }
      if (url.endsWith("/titles") && !url.includes("?")) {
        return jsonResponse(dune, 201);
      }
      if (url.includes("/titles/")) {
        if (url.endsWith("/titles/title-1")) {
          return { ok: true, status: 204, json: async () => ({}) };
        }
        return jsonResponse(dune);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await login({ username: "editor", password: "password1" });
    await logout();
    await createTitle(write);
    await updateTitle(dune.id, write);
    await deleteTitle("title-1");

    assertCsrfImmediatelyBeforeEachUnsafe(fetchMock);
    assertCmsApiOnly(fetchMock);
  });

  it("does not reuse a CSRF token across two unsafe calls", async () => {
    let csrfCount = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        csrfCount += 1;
        return jsonResponse({
          headerName: "X-WATCHNEST-CMS-XSRF-TOKEN",
          token: `csrf-${csrfCount}`,
        });
      }
      if (url.endsWith("/titles") && (init?.method ?? "GET").toUpperCase() === "POST") {
        return jsonResponse(dune, 201);
      }
      if (url.includes("/titles/") && (init?.method ?? "GET").toUpperCase() === "PUT") {
        return jsonResponse(dune);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await createTitle(write);
    await updateTitle(dune.id, write);

    const csrfCalls = fetchMock.mock.calls.filter(([input]) => String(input).includes("/csrf"));
    expect(csrfCalls).toHaveLength(2);
    expect(csrfCalls[0]?.[1]).toEqual(
      expect.objectContaining({ credentials: "include", cache: "no-store" }),
    );
    const createCall = fetchMock.mock.calls.find(
      ([input, init]) => String(input).endsWith("/titles") && (init?.method ?? "GET") === "POST",
    );
    const updateCall = fetchMock.mock.calls.find(
      ([input, init]) => String(input).includes(`/titles/${dune.id}`) && init?.method === "PUT",
    );
    expect(new Headers(createCall?.[1]?.headers).get("X-WATCHNEST-CMS-XSRF-TOKEN")).toBe("csrf-1");
    expect(new Headers(updateCall?.[1]?.headers).get("X-WATCHNEST-CMS-XSRF-TOKEN")).toBe("csrf-2");
    assertCsrfImmediatelyBeforeEachUnsafe(fetchMock);
    assertCmsApiOnly(fetchMock);
  });

  it("uses GET with q for title search and omits CSRF", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (url.includes("/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.match(/\/titles\/[^/?]+/) && method === "GET") {
        return jsonResponse(dune);
      }
      if (url.includes("/titles") && method === "GET") {
        return jsonResponse({ titles: [dune] });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchTitles("Dune")).resolves.toEqual({ titles: [dune] });
    await expect(fetchTitles("  ")).resolves.toEqual({ titles: [dune] });
    await expect(fetchTitle(dune.id)).resolves.toEqual(dune);

    expect(fetchMock).toHaveBeenCalledWith(
      "/cms/api/v1/titles?q=Dune",
      expect.objectContaining({ method: "GET", credentials: "include" }),
    );
    expect(fetchMock).toHaveBeenCalledWith(
      "/cms/api/v1/titles",
      expect.objectContaining({ method: "GET", credentials: "include" }),
    );
    expect(fetchMock).toHaveBeenCalledWith(
      `/cms/api/v1/titles/${dune.id}`,
      expect.objectContaining({ method: "GET", credentials: "include" }),
    );
    const searchCall = fetchMock.mock.calls.find(([input]) => String(input).includes("q=Dune"));
    expect(new Headers(searchCall?.[1]?.headers).get("X-WATCHNEST-CMS-XSRF-TOKEN")).toBeNull();
    assertCmsApiOnly(fetchMock);
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

  it("returns the current CMS user from /me", async () => {
    const user = { id: "1", username: "editor" };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(user)));

    await expect(fetchMe()).resolves.toEqual(user);
  });

  it("preserves HTTP status, error code, and 409 existingTitle", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        return jsonResponse(csrf);
      }
      return jsonResponse(
        {
          code: "title_already_exists",
          message: "A title with the same English name, year, and type already exists",
          existingTitle: dune,
        },
        409,
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createTitle(write)).rejects.toMatchObject({
      status: 409,
      code: "title_already_exists",
      message: "A title with the same English name, year, and type already exists",
      existingTitle: dune,
    });
    assertCmsApiOnly(fetchMock);
  });

  it("retries once after csrf_invalid", async () => {
    let createAttempts = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        return jsonResponse({
          headerName: "X-WATCHNEST-CMS-XSRF-TOKEN",
          token: `csrf-${fetchMock.mock.calls.filter(([u]) => String(u).includes("/csrf")).length}`,
        });
      }
      if (url.endsWith("/titles")) {
        createAttempts += 1;
        if (createAttempts === 1) {
          return jsonResponse({ code: "csrf_invalid", message: "CSRF" }, 403);
        }
        return jsonResponse(dune, 201);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createTitle(write)).resolves.toEqual(dune);
    expect(createAttempts).toBe(2);
    assertCsrfImmediatelyBeforeEachUnsafe(fetchMock);
    assertCmsApiOnly(fetchMock);
  });

  it("does not retry 403 demo_account", async () => {
    let createAttempts = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.endsWith("/titles")) {
        createAttempts += 1;
        return jsonResponse(
          {
            code: "demo_account",
            message: "This is a demonstration account. The change was not applied.",
          },
          403,
        );
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createTitle(write)).rejects.toMatchObject({
      status: 403,
      code: "demo_account",
      message: "This is a demonstration account. The change was not applied.",
    });
    expect(createAttempts).toBe(1);
    const titleCalls = fetchMock.mock.calls.filter(
      ([input]) => String(input).endsWith("/titles") && !String(input).includes("/csrf"),
    );
    expect(titleCalls).toHaveLength(1);
    assertCmsApiOnly(fetchMock);
  });

  it("does not retry csrf_invalid more than once", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        return jsonResponse(csrf);
      }
      if (url.endsWith("/titles")) {
        return jsonResponse({ code: "csrf_invalid", message: "CSRF" }, 403);
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(createTitle(write)).rejects.toMatchObject({ code: "csrf_invalid", status: 403 });

    const titleCalls = fetchMock.mock.calls.filter(
      ([input]) => String(input).endsWith("/titles") && !String(input).includes("/csrf"),
    );
    expect(titleCalls).toHaveLength(2);
  });

  it("keeps login success when post-auth CSRF refresh fails", async () => {
    let csrfCalls = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/csrf")) {
        csrfCalls += 1;
        if (csrfCalls === 1) {
          return jsonResponse(csrf);
        }
        return jsonResponse({ code: "request_failed", message: "csrf down" }, 500);
      }
      if (url.endsWith("/login")) {
        return jsonResponse({ id: "1", username: "editor" });
      }
      return jsonResponse({}, 404);
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(login({ username: "editor", password: "password1" })).resolves.toEqual({
      id: "1",
      username: "editor",
    });
    assertCmsApiOnly(fetchMock);
  });
});
