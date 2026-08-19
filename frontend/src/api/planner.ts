import { apiRequest } from "./http";
import type {
  Dashboard,
  ForwardPlan,
  ForwardPlanItem,
  PlanTodayLine,
  ScreenTimePolicy,
  WatchEvent,
  WatchEventArchive,
  LibraryPreferences,
} from "../types";

export async function fetchDashboard(): Promise<Dashboard> {
  return apiRequest<Dashboard>("/api/v1/dashboard");
}

export async function fetchWatchEvents(from: string, to: string): Promise<WatchEventArchive> {
  const params = new URLSearchParams({ from, to });
  return apiRequest<WatchEventArchive>(`/api/v1/watch-events?${params.toString()}`);
}

export async function addWatchEvent(watchedOn: string, contentTitle: string): Promise<WatchEvent> {
  return apiRequest<WatchEvent>("/api/v1/watch-events", {
    method: "POST",
    body: JSON.stringify({ watchedOn, contentTitle }),
  });
}

export async function patchWatchEvent(id: string, contentTitle: string): Promise<WatchEvent> {
  return apiRequest<WatchEvent>(`/api/v1/watch-events/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ contentTitle }),
  });
}

export async function deleteWatchEvent(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/watch-events/${id}`, {
    method: "DELETE",
  });
}

export async function addPlanTodayLine(contentTitle: string): Promise<PlanTodayLine> {
  return apiRequest<PlanTodayLine>("/api/v1/plan/today/lines", {
    method: "POST",
    body: JSON.stringify({ contentTitle }),
  });
}

export async function patchPlanTodayLine(id: string, checked: boolean): Promise<PlanTodayLine> {
  return apiRequest<PlanTodayLine>(`/api/v1/plan/today/lines/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ checked }),
  });
}

export async function deletePlanTodayLine(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/plan/today/lines/${id}`, {
    method: "DELETE",
  });
}

export async function fetchForwardPlan(from: string, to: string): Promise<ForwardPlan> {
  const params = new URLSearchParams({ from, to });
  return apiRequest<ForwardPlan>(`/api/v1/plan/forward?${params.toString()}`);
}

export async function addForwardPlanItem(
  plannedFor: string,
  contentTitle: string,
): Promise<ForwardPlanItem> {
  return apiRequest<ForwardPlanItem>("/api/v1/plan/forward", {
    method: "POST",
    body: JSON.stringify({ plannedFor, contentTitle }),
  });
}

export async function deleteForwardPlanItem(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/plan/forward/${id}`, {
    method: "DELETE",
  });
}

export async function updatePolicy(policy: ScreenTimePolicy): Promise<ScreenTimePolicy> {
  return apiRequest<ScreenTimePolicy>("/api/v1/policy", {
    method: "PUT",
    body: JSON.stringify(policy),
  });
}

export async function updateLibraryPreferences(
  preferences: LibraryPreferences,
): Promise<LibraryPreferences> {
  return apiRequest<LibraryPreferences>("/api/v1/library-preferences", {
    method: "PUT",
    body: JSON.stringify(preferences),
  });
}
