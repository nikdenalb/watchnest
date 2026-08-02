import { apiRequest, clearCsrfCache, fetchCsrf, refreshCsrf } from "./http";
import { ApiError } from "./errors";
import type { CurrentUser } from "../types";

export type Credentials = {
  username: string;
  password: string;
};

export { fetchCsrf, refreshCsrf };

/** Best-effort CSRF refresh; session success must not depend on this call. */
async function refreshCsrfAfterAuthChange(): Promise<void> {
  try {
    await refreshCsrf();
  } catch {
    clearCsrfCache();
  }
}

export async function fetchMe(): Promise<CurrentUser | null> {
  try {
    return await apiRequest<CurrentUser>("/api/v1/auth/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function register(credentials: Credentials): Promise<CurrentUser> {
  const user = await apiRequest<CurrentUser>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
  await refreshCsrfAfterAuthChange();
  return user;
}

export async function login(credentials: Credentials): Promise<CurrentUser> {
  const user = await apiRequest<CurrentUser>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
  await refreshCsrfAfterAuthChange();
  return user;
}

export async function logout(): Promise<void> {
  try {
    await apiRequest<void>("/api/v1/auth/logout", {
      method: "POST",
    });
  } finally {
    await refreshCsrfAfterAuthChange();
  }
}
