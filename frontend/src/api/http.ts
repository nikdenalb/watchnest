import { ApiError } from "./errors";
import type { CsrfToken } from "../types";

let csrf: CsrfToken | null = null;

/** Clears cached CSRF (tests and after intentional session teardown). */
export function clearCsrfCache(): void {
  csrf = null;
}

async function parseApiError(response: Response): Promise<ApiError> {
  let code = "request_failed";
  let message = `Request failed with status ${response.status}`;
  try {
    const body = (await response.json()) as { code?: unknown; message?: unknown };
    if (typeof body.code === "string" && body.code.length > 0) {
      code = body.code;
    }
    if (typeof body.message === "string" && body.message.length > 0) {
      message = body.message;
    }
  } catch {
    // non-JSON error bodies still become ApiError with status
  }
  return new ApiError(response.status, code, message);
}

export async function fetchCsrf(): Promise<CsrfToken> {
  const response = await fetch("/api/v1/auth/csrf", {
    credentials: "include",
  });
  if (!response.ok) {
    throw await parseApiError(response);
  }
  const token = (await response.json()) as CsrfToken;
  csrf = token;
  return token;
}

export async function refreshCsrf(): Promise<CsrfToken> {
  clearCsrfCache();
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
 * Session-aware JSON request: always sends cookies; attaches CSRF on unsafe methods;
 * refreshes CSRF and retries once on `csrf_invalid`.
 */
export async function apiRequest<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();
  const allowCsrfRetry = options.allowCsrfRetry !== false;
  const headers = new Headers(options.headers);

  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (isUnsafeMethod(method)) {
    if (!csrf) {
      await fetchCsrf();
    }
    if (csrf) {
      headers.set(csrf.headerName, csrf.token);
    }
  }

  const response = await fetch(url, {
    method,
    headers,
    body: options.body,
    credentials: "include",
  });

  if (
    response.status === 403 &&
    isUnsafeMethod(method) &&
    allowCsrfRetry
  ) {
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
