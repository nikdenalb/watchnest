import type { QueryClient } from "@tanstack/react-query";

export const ME_QUERY_KEY = ["me"] as const;
export const WATCH_EVENTS_QUERY_KEY = ["watch-events"] as const;

export function watchEventsQueryKey(from: string, to: string) {
  return ["watch-events", from, to] as const;
}

export function invalidateWatchEvents(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: WATCH_EVENTS_QUERY_KEY });
}

/** Drop authenticated client state so the UI returns to the auth screen. */
export function clearUserScopedQueries(queryClient: QueryClient) {
  queryClient.setQueryData(ME_QUERY_KEY, null);
  queryClient.removeQueries({ queryKey: WATCH_EVENTS_QUERY_KEY });
}
