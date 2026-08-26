import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearCsrfCache } from "./api/http";
import { App } from "./App";
import type { CatalogTitle, CmsUser, TitleType, TitleWrite } from "./types";

const editor: CmsUser = { id: "00000000-0000-0000-0000-000000000001", username: "editor" };

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

const arrival: CatalogTitle = {
  id: "00000000-0000-0000-0000-000000000002",
  type: "FILM",
  nameEn: "Arrival",
  nameOriginal: "Arrival",
  year: 2016,
  description: "Language",
  genres: "drama, science fiction",
  countries: "united states",
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

function nameEnKey(name: string) {
  return name.trim().toLowerCase();
}

function normalizeTags(value: string | null | undefined): string | null {
  if (value == null) {
    return null;
  }
  const parts = value
    .split(",")
    .map((token) => token.trim().toLowerCase())
    .filter((token) => token.length > 0);
  return parts.length === 0 ? null : parts.join(", ");
}

function optionalText(value: string | null | undefined): string | null {
  if (value == null) {
    return null;
  }
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function identityKey(title: { nameEn: string; year: number; type: string }) {
  return `${nameEnKey(title.nameEn)}|${title.year}|${title.type}`;
}

function sortTitles(titles: CatalogTitle[]) {
  return [...titles].sort((a, b) => {
    const name = nameEnKey(a.nameEn).localeCompare(nameEnKey(b.nameEn));
    if (name !== 0) {
      return name;
    }
    if (a.year !== b.year) {
      return a.year - b.year;
    }
    const type = a.type.localeCompare(b.type);
    if (type !== 0) {
      return type;
    }
    return a.id.localeCompare(b.id);
  });
}

function canonicalize(fields: TitleWrite, id: string): CatalogTitle {
  return {
    id,
    type: fields.type,
    nameEn: fields.nameEn.trim(),
    nameOriginal: fields.nameOriginal.trim(),
    year: fields.year,
    description: optionalText(fields.description),
    genres: normalizeTags(fields.genres),
    countries: normalizeTags(fields.countries),
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

function assertCmsApiOnly(fetchMock: ReturnType<typeof vi.fn>) {
  expect(fetchMock.mock.calls.length).toBeGreaterThan(0);
  for (const [input] of fetchMock.mock.calls) {
    const url = String(input);
    expect(url.startsWith("/cms/api/v1")).toBe(true);
    expect(url.includes("/api/v1/auth")).toBe(false);
  }
}

describe("CMS app", () => {
  let session: CmsUser | null;
  let titles: CatalogTitle[];
  let nextId: number;
  let fetchImpl: FetchImpl;
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    clearCsrfCache();
    session = null;
    titles = [];
    nextId = 10;

    fetchImpl = async (input, init) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      const parsed = new URL(url, "http://localhost");

      if (parsed.pathname === "/cms/api/v1/csrf") {
        return jsonResponse({ headerName: "X-WATCHNEST-CMS-XSRF-TOKEN", token: "cms-csrf-test" });
      }

      if (parsed.pathname === "/cms/api/v1/me") {
        if (!session) {
          return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
        }
        return jsonResponse(session);
      }

      if (parsed.pathname === "/cms/api/v1/login" && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as {
          username?: string;
          password?: string;
        };
        if (body.password !== "password1" || body.username?.toLowerCase() !== "editor") {
          return jsonResponse({ code: "invalid_credentials", message: "Invalid credentials" }, 401);
        }
        session = editor;
        return jsonResponse(session);
      }

      if (parsed.pathname === "/cms/api/v1/logout" && method === "POST") {
        session = null;
        return { ok: true, status: 204, json: async () => ({}) };
      }

      const titleItem = parsed.pathname.match(/^\/cms\/api\/v1\/titles\/([^/]+)$/);

      if (!session) {
        return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
      }

      if (parsed.pathname === "/cms/api/v1/titles" && method === "GET") {
        const q = (parsed.searchParams.get("q") ?? "").trim().toLowerCase();
        const matched = q
          ? titles.filter((title) => title.nameEn.toLowerCase().includes(q))
          : titles;
        return jsonResponse({ titles: sortTitles(matched) });
      }

      if (titleItem && method === "GET") {
        const found = titles.find((title) => title.id === titleItem[1]);
        if (!found) {
          return jsonResponse({ code: "not_found", message: "Not found" }, 404);
        }
        return jsonResponse(found);
      }

      if (parsed.pathname === "/cms/api/v1/titles" && method === "POST") {
        const body = JSON.parse(String(init?.body ?? "{}")) as TitleWrite;
        if (body.nameEn === "Invalid") {
          return jsonResponse({ code: "validation_failed", message: "Year is required" }, 400);
        }
        const created = canonicalize(body, `title-${nextId++}`);
        const existing = titles.find((title) => identityKey(title) === identityKey(created));
        if (existing) {
          return jsonResponse(
            {
              code: "title_already_exists",
              message: "A title with the same English name, year, and type already exists",
              existingTitle: existing,
            },
            409,
          );
        }
        titles = [...titles, created];
        return jsonResponse(created, 201);
      }

      if (titleItem && method === "PUT") {
        const body = JSON.parse(String(init?.body ?? "{}")) as TitleWrite;
        const index = titles.findIndex((title) => title.id === titleItem[1]);
        if (index < 0) {
          return jsonResponse({ code: "not_found", message: "Not found" }, 404);
        }
        const updated = canonicalize(body, titleItem[1]);
        const existing = titles.find(
          (title) => title.id !== updated.id && identityKey(title) === identityKey(updated),
        );
        if (existing) {
          return jsonResponse(
            {
              code: "title_already_exists",
              message: "A title with the same English name, year, and type already exists",
              existingTitle: existing,
            },
            409,
          );
        }
        titles = titles.map((title) => (title.id === updated.id ? updated : title));
        return jsonResponse(updated);
      }

      if (titleItem && method === "DELETE") {
        const index = titles.findIndex((title) => title.id === titleItem[1]);
        if (index < 0) {
          return jsonResponse({ code: "not_found", message: "Not found" }, 404);
        }
        titles = titles.filter((title) => title.id !== titleItem[1]);
        return { ok: true, status: 204, json: async () => ({}) };
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

  async function signIn(username = "editor", password = "password1") {
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Username"), username);
    await user.type(screen.getByLabelText("Password"), password);
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    return user;
  }

  async function fillTitleForm(
    user: ReturnType<typeof userEvent.setup>,
    fields: {
      type?: TitleType;
      nameEn: string;
      nameOriginal: string;
      year: string;
      description?: string;
      genres?: string;
      countries?: string;
    },
  ) {
    if (fields.type) {
      await user.selectOptions(screen.getByLabelText("Type"), fields.type);
    }
    await user.clear(screen.getByLabelText("English name"));
    await user.type(screen.getByLabelText("English name"), fields.nameEn);
    await user.clear(screen.getByLabelText("Original name"));
    await user.type(screen.getByLabelText("Original name"), fields.nameOriginal);
    await user.clear(screen.getByLabelText("Year"));
    await user.type(screen.getByLabelText("Year"), fields.year);
    if (fields.description !== undefined) {
      await user.clear(screen.getByLabelText("Description"));
      if (fields.description.length > 0) {
        await user.type(screen.getByLabelText("Description"), fields.description);
      }
    }
    if (fields.genres !== undefined) {
      await user.clear(screen.getByLabelText("Genres"));
      if (fields.genres.length > 0) {
        await user.type(screen.getByLabelText("Genres"), fields.genres);
      }
    }
    if (fields.countries !== undefined) {
      await user.clear(screen.getByLabelText("Countries"));
      if (fields.countries.length > 0) {
        await user.type(screen.getByLabelText("Countries"), fields.countries);
      }
    }
  }

  it("shows pending session check on startup", async () => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    fetchImpl = async (input) => {
      const url = String(input);
      if (url.includes("/me")) {
        await gate;
        return jsonResponse({ code: "authentication_required", message: "Auth required" }, 401);
      }
      return jsonResponse({}, 404);
    };

    renderApp();
    expect(screen.getByText("Checking session...")).toBeInTheDocument();
    release();
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });

  it("shows an error when session check fails", async () => {
    fetchImpl = async (input) => {
      if (String(input).includes("/me")) {
        return jsonResponse({ code: "request_failed", message: "down" }, 500);
      }
      return jsonResponse({}, 404);
    };

    renderApp();
    expect(await screen.findByText("Failed to resolve authentication state.")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Sign in" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Catalog" })).not.toBeInTheDocument();
  });

  it("shows sign-in when /me is 401", async () => {
    renderApp();
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Catalog" })).not.toBeInTheDocument();
    assertCmsApiOnly(fetchMock);
  });

  it("shows the catalog when already authenticated", async () => {
    session = editor;
    titles = [dune];
    renderApp();
    expect(await screen.findByRole("heading", { name: "Catalog" })).toBeInTheDocument();
    expect(screen.getByText("editor")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Dune/ })).toBeInTheDocument();
    expect(screen.getByText("drama, science fiction")).toBeInTheDocument();
    expect(screen.getByText("united states, canada")).toBeInTheDocument();
  });

  it("is sign-in only and has no registration control", async () => {
    renderApp();
    await screen.findByRole("heading", { name: "Sign in" });
    expect(screen.queryByRole("tab")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /register/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /create account/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/confirm password/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/register/i)).not.toBeInTheDocument();
  });

  it("shows a generic message for invalid credentials", async () => {
    renderApp();
    await screen.findByRole("heading", { name: "Sign in" });
    await signIn("editor", "wrongpass");
    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password.");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.queryByRole("heading", { name: "Catalog" })).not.toBeInTheDocument();
  });

  it("shows validation errors from title create", async () => {
    session = editor;
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });
    await fillTitleForm(user, {
      nameEn: "Invalid",
      nameOriginal: "Invalid",
      year: "2021",
    });
    await user.click(screen.getByRole("button", { name: "Create" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Year is required");
  });

  it("disables sign-in controls while pending", async () => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    const original = fetchImpl;
    fetchImpl = async (input, init) => {
      const url = String(input);
      if (url.endsWith("/login")) {
        await gate;
        return original(input, init);
      }
      return original(input, init);
    };

    renderApp();
    await screen.findByRole("heading", { name: "Sign in" });
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Username"), "editor");
    await user.type(screen.getByLabelText("Password"), "password1");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(screen.getByRole("button", { name: "Sign in" })).toBeDisabled();
    expect(screen.getByLabelText("Username")).toBeDisabled();
    expect(screen.getByLabelText("Password")).toBeDisabled();
    release();
    expect(await screen.findByRole("heading", { name: "Catalog" })).toBeInTheDocument();
  });

  it("exposes labeled catalog fields, type enum options, and alerts", async () => {
    session = editor;
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });

    expect(screen.getByLabelText("Search English name")).toBeInTheDocument();
    expect(screen.getByLabelText("Type")).toBeInTheDocument();
    expect(screen.getByLabelText("English name")).toBeInTheDocument();
    expect(screen.getByLabelText("Original name")).toBeInTheDocument();
    expect(screen.getByLabelText("Year")).toBeInTheDocument();
    expect(screen.getByLabelText("Description")).toBeInTheDocument();
    expect(screen.getByLabelText("Genres")).toBeInTheDocument();
    expect(screen.getByLabelText("Countries")).toBeInTheDocument();

    const type = screen.getByLabelText("Type");
    expect(within(type).getByRole("option", { name: "FILM" })).toBeInTheDocument();
    expect(within(type).getByRole("option", { name: "TV_SERIES" })).toBeInTheDocument();
    expect(within(type).getByRole("option", { name: "MINI_SERIES" })).toBeInTheDocument();
    expect(within(type).getByRole("option", { name: "TV_SHOW" })).toBeInTheDocument();
  });

  it("logs out, clears catalog cache, and returns to sign-in", async () => {
    session = editor;
    titles = [dune];
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("button", { name: /Dune/ });

    await user.click(screen.getByRole("button", { name: "Log out" }));

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("Dune")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Catalog" })).not.toBeInTheDocument();
  });

  it("shows an empty catalog", async () => {
    session = editor;
    renderApp();
    expect(await screen.findByText("No titles in the catalog.")).toBeInTheDocument();
  });

  it("searches by English name and resets to all", async () => {
    session = editor;
    titles = [dune, arrival];
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("button", { name: /Dune/ });
    expect(screen.getByRole("button", { name: /Arrival/ })).toBeInTheDocument();

    await user.type(screen.getByLabelText("Search English name"), "dune");
    await user.click(screen.getByRole("button", { name: "Search" }));

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: /Arrival/ })).not.toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: /Dune/ })).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([input]) => String(input) === "/cms/api/v1/titles?q=dune")).toBe(
      true,
    );

    await user.clear(screen.getByLabelText("Search English name"));
    await user.click(screen.getByRole("button", { name: "Search" }));
    expect(await screen.findByRole("button", { name: /Arrival/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Dune/ })).toBeInTheDocument();
  });

  it("creates a title, sends the request, and renders the result", async () => {
    session = editor;
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });

    await fillTitleForm(user, {
      type: "TV_SERIES",
      nameEn: "The Expanse",
      nameOriginal: "The Expanse",
      year: "2015",
      description: "Belters",
      genres: "science fiction",
      countries: "united states",
    });
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByRole("button", { name: /The Expanse/ })).toBeInTheDocument();
    const createCall = fetchMock.mock.calls.find(
      ([input, init]) => String(input) === "/cms/api/v1/titles" && (init?.method ?? "GET") === "POST",
    );
    expect(JSON.parse(String(createCall?.[1]?.body))).toEqual({
      type: "TV_SERIES",
      nameEn: "The Expanse",
      nameOriginal: "The Expanse",
      year: 2015,
      description: "Belters",
      genres: "science fiction",
      countries: "united states",
    });
    expect(
      fetchMock.mock.calls.filter(([input, init]) => {
        return String(input) === "/cms/api/v1/titles" && (init?.method ?? "GET") === "GET";
      }).length,
    ).toBeGreaterThanOrEqual(2);
  });

  it("sends null optionals and does not render the string null", async () => {
    session = editor;
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });
    await fillTitleForm(user, {
      nameEn: "Blank Optional",
      nameOriginal: "Blank Optional",
      year: "2020",
    });
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByRole("button", { name: /Blank Optional/ })).toBeInTheDocument();
    const createCall = fetchMock.mock.calls.find(
      ([input, init]) => String(input) === "/cms/api/v1/titles" && (init?.method ?? "GET") === "POST",
    );
    expect(JSON.parse(String(createCall?.[1]?.body))).toEqual({
      type: "FILM",
      nameEn: "Blank Optional",
      nameOriginal: "Blank Optional",
      year: 2020,
      description: null,
      genres: null,
      countries: null,
    });
    expect(screen.queryByText("null")).not.toBeInTheDocument();
  });

  it("renders server-normalized genres and countries", async () => {
    session = editor;
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });
    await fillTitleForm(user, {
      nameEn: "Dune",
      nameOriginal: "Dune",
      year: "2021",
      genres: "Drama, SCIENCE Fiction",
      countries: "United States, Canada",
    });
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText("drama, science fiction")).toBeInTheDocument();
    expect(screen.getByText("united states, canada")).toBeInTheDocument();
  });

  it("edits a selected title with a full replacement PUT", async () => {
    session = editor;
    titles = [{ ...dune }];
    const user = userEvent.setup();
    renderApp();
    await user.click(await screen.findByRole("button", { name: /Dune/ }));
    expect(await screen.findByRole("heading", { name: "Edit title" })).toBeInTheDocument();

    await fillTitleForm(user, {
      type: "MINI_SERIES",
      nameEn: "Dune",
      nameOriginal: "Dune: Part One",
      year: "2021",
      description: "Arrakis",
      genres: "Drama",
      countries: "United States",
    });
    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Dune: Part One/ })).toBeInTheDocument();
    });
    const putCall = fetchMock.mock.calls.find(
      ([input, init]) => String(input) === `/cms/api/v1/titles/${dune.id}` && init?.method === "PUT",
    );
    expect(JSON.parse(String(putCall?.[1]?.body))).toEqual({
      type: "MINI_SERIES",
      nameEn: "Dune",
      nameOriginal: "Dune: Part One",
      year: 2021,
      description: "Arrakis",
      genres: "Drama",
      countries: "United States",
    });
  });

  it("confirms delete, can cancel, then deletes", async () => {
    session = editor;
    titles = [{ ...dune }];
    const user = userEvent.setup();
    renderApp();
    await user.click(await screen.findByRole("button", { name: /Dune/ }));
    await user.click(screen.getByRole("button", { name: "Delete" }));

    const dialog = await screen.findByRole("dialog");
    expect(dialog).toHaveTextContent("Delete “Dune”");
    await user.click(within(dialog).getByRole("button", { name: "Cancel" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Dune/ })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Delete" }));
    const confirm = await screen.findByRole("dialog");
    await user.click(within(confirm).getByRole("button", { name: "Delete" }));

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: /Dune/ })).not.toBeInTheDocument();
    });
    expect(screen.getByText("No titles in the catalog.")).toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([input, init]) => String(input) === `/cms/api/v1/titles/${dune.id}` && init?.method === "DELETE",
      ),
    ).toBe(true);
  });

  it("renders 409 existingTitle instead of a generic error", async () => {
    session = editor;
    titles = [{ ...dune }];
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });
    await fillTitleForm(user, {
      nameEn: "dune",
      nameOriginal: "Dune",
      year: "2021",
    });
    await user.click(screen.getByRole("button", { name: "Create" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(
      "A title with the same English name, year, and type already exists",
    );
    expect(alert).toHaveTextContent("Dune");
    expect(alert).toHaveTextContent("2021");
    expect(alert).toHaveTextContent("FILM");
    expect(alert).toHaveTextContent(dune.id);
    expect(alert).toHaveTextContent("drama, science fiction");
    expect(alert).not.toHaveTextContent("Something went wrong");
  });

  it("returns to sign-in when a title mutation is 401", async () => {
    session = editor;
    const user = userEvent.setup();
    renderApp();
    await screen.findByRole("heading", { name: "Create title" });
    await fillTitleForm(user, {
      nameEn: "Late",
      nameOriginal: "Late",
      year: "2022",
    });
    session = null;
    await user.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    });
    expect(screen.queryByRole("heading", { name: "Catalog" })).not.toBeInTheDocument();
  });
});
