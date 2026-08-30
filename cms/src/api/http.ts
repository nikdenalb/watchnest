import { ApiError } from "./errors";
import type { CatalogTitle, CsrfToken, TitleType } from "../types";

const CMS_API_PREFIX = "/cms/api/v1";

/** No-op: CMS CSRF is not cached. Kept for auth and test call sites. */
export function clearCsrfCache(): void {}

function isTitleType(value: unknown): value is TitleType {
  return value === "FILM" || value === "TV_SERIES" || value === "MINI_SERIES" || value === "TV_SHOW";
}

function parseExistingTitle(value: unknown): CatalogTitle | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }
  const body = value as Record<string, unknown>;
  if (
    typeof body.id !== "string" ||
    !isTitleType(body.type) ||
    typeof body.nameEn !== "string" ||
    typeof body.nameOriginal !== "string" ||
    typeof body.year !== "number"
  ) {
    return undefined;
  }
  return {
    id: body.id,
    type: body.type,
    nameEn: body.nameEn,
    nameOriginal: body.nameOriginal,
    year: body.year,
    description: typeof body.description === "string" ? body.description : null,
    genres: typeof body.genres === "string" ? body.genres : null,
    countries: typeof body.countries === "string" ? body.countries : null,
  };
}

async function parseApiError(response: Response): Promise<ApiError> {
  let code = "request_failed";
  let message = `Request failed with status ${response.status}`;
  let existingTitle: CatalogTitle | undefined;
  try {
    const body = (await response.json()) as {
      code?: unknown;
      message?: unknown;
      existingTitle?: unknown;
    };
    if (typeof body.code === "string" && body.code.length > 0) {
      code = body.code;
    }
    if (typeof body.message === "string" && body.message.length > 0) {
      message = body.message;
    }
    existingTitle = parseExistingTitle(body.existingTitle);
  } catch {
    // non-JSON error bodies still become ApiError with status
  }
  return new ApiError(response.status, code, message, existingTitle);
}

export async function fetchCsrf(): Promise<CsrfToken> {
  const response = await fetch(`${CMS_API_PREFIX}/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) {
    throw await parseApiError(response);
  }
  return (await response.json()) as CsrfToken;
}

export async function refreshCsrf(): Promise<CsrfToken> {
  return fetchCsrf();
}

function isUnsafeMethod(method: string): boolean {
  return !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method.toUpperCase());
}

type RequestOptions = {
  method?: string;
  body?: string;
  headers?: HeadersInit;
  /** When false, skip one-time CSRF retry (internal). */
  allowCsrfRetry?: boolean;
};

/**
 * Session-aware JSON request: always sends cookies; fetches CMS CSRF immediately
 * before each unsafe method and sends that response’s header; retries once on
 * `csrf_invalid`.
 */
export async function apiRequest<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const allowCsrfRetry = options.allowCsrfRetry !== false;
  const headers = new Headers(options.headers);

  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (isUnsafeMethod(method)) {
    const token = await fetchCsrf();
    headers.set(token.headerName, token.token);
  }

  const response = await fetch(url, {
    method,
    headers,
    body: options.body,
    credentials: "include",
    cache: "no-store",
  });

  if (response.status === 403 && isUnsafeMethod(method) && allowCsrfRetry) {
    const error = await parseApiError(response);
    if (error.code === "csrf_invalid") {
      await refreshCsrf();
      return apiRequest<T>(url, { ...options, allowCsrfRetry: false });
    }
    throw error;
  }

  if (!response.ok) {
    throw await parseApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
