import { apiRequest } from "./http";
import type { WatchEvent, WatchEventRange } from "../types";

export async function fetchWatchEvents(from: string, to: string): Promise<WatchEventRange> {
  const params = new URLSearchParams({ from, to });
  return apiRequest<WatchEventRange>(`/api/v1/watch-events?${params.toString()}`);
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
