import type { Dashboard, ScreenTimePolicy, WatchEvent } from "../types";

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function fetchDashboard(): Promise<Dashboard> {
  const response = await fetch("/api/v1/dashboard");
  return readJson<Dashboard>(response);
}

export async function logWatchEvent(contentTitle: string): Promise<WatchEvent> {
  const response = await fetch("/api/v1/watch-events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ contentTitle }),
  });
  return readJson<WatchEvent>(response);
}

export async function updatePolicy(policy: ScreenTimePolicy): Promise<ScreenTimePolicy> {
  const response = await fetch("/api/v1/policy", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(policy),
  });
  return readJson<ScreenTimePolicy>(response);
}
