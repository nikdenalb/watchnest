import type { QueryClient } from "@tanstack/react-query";

export const ME_QUERY_KEY = ["cms-me"] as const;
export const TITLES_QUERY_KEY = ["cms-titles"] as const;

export function titlesQueryKey(q: string) {
  return ["cms-titles", q] as const;
}

export function invalidateTitlesQueries(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: TITLES_QUERY_KEY });
}

/** Drop authenticated CMS client state so the UI returns to sign-in. */
export function clearCmsScopedQueries(queryClient: QueryClient) {
  queryClient.setQueryData(ME_QUERY_KEY, null);
  queryClient.removeQueries({ queryKey: TITLES_QUERY_KEY });
}
