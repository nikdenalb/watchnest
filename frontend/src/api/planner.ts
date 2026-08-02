import { apiRequest } from "./http";
import type { Dashboard, ScreenTimePolicy, WatchEvent } from "../types";

export async function fetchDashboard(): Promise<Dashboard> {
  return apiRequest<Dashboard>("/api/v1/dashboard");
}

export async function logWatchEvent(contentTitle: string): Promise<WatchEvent> {
  return apiRequest<WatchEvent>("/api/v1/watch-events", {
    method: "POST",
    body: JSON.stringify({ contentTitle }),
  });
}

export async function updatePolicy(policy: ScreenTimePolicy): Promise<ScreenTimePolicy> {
  return apiRequest<ScreenTimePolicy>("/api/v1/policy", {
    method: "PUT",
    body: JSON.stringify(policy),
  });
}
