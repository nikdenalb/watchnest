import { apiRequest, clearCsrfCache, fetchCsrf, refreshCsrf } from "./http";
import { ApiError } from "./errors";
import type { CmsUser } from "../types";

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

export async function fetchMe(): Promise<CmsUser | null> {
  try {
    return await apiRequest<CmsUser>("/cms/api/v1/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function login(credentials: Credentials): Promise<CmsUser> {
  const user = await apiRequest<CmsUser>("/cms/api/v1/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
  await refreshCsrfAfterAuthChange();
  return user;
}

export async function logout(): Promise<void> {
  try {
    await apiRequest<void>("/cms/api/v1/logout", {
      method: "POST",
    });
  } finally {
    await refreshCsrfAfterAuthChange();
  }
}
