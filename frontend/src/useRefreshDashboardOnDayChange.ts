import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";

const DAY_CHECK_INTERVAL_MS = 60_000;

/** Local calendar date as `YYYY-MM-DD` (matches API `LocalDate` JSON). */
export function localDateIso(date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function msUntilNextLocalMidnight(now = new Date()): number {
  const nextMidnight = new Date(now);
  nextMidnight.setHours(24, 0, 0, 0);
  return Math.max(1, nextMidnight.getTime() - now.getTime());
}

/**
 * Keeps the dashboard aligned with the local calendar day while the tab stays open:
 * refetch on focus/visibility, every minute if the day drifted, and at local midnight.
 */
export function useRefreshDashboardOnDayChange(anchoredToday: string | undefined) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!anchoredToday) {
      return;
    }

    const refreshIfDayChanged = () => {
      if (localDateIso() !== anchoredToday) {
        void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      }
    };

    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        refreshIfDayChanged();
      }
    };

    window.addEventListener("focus", refreshIfDayChanged);
    document.addEventListener("visibilitychange", onVisibilityChange);

    const intervalId = window.setInterval(refreshIfDayChanged, DAY_CHECK_INTERVAL_MS);

    let midnightTimerId = 0;
    const scheduleMidnightRefresh = () => {
      midnightTimerId = window.setTimeout(() => {
        void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
        scheduleMidnightRefresh();
      }, msUntilNextLocalMidnight());
    };
    scheduleMidnightRefresh();

    return () => {
      window.removeEventListener("focus", refreshIfDayChanged);
      document.removeEventListener("visibilitychange", onVisibilityChange);
      window.clearInterval(intervalId);
      window.clearTimeout(midnightTimerId);
    };
  }, [anchoredToday, queryClient]);
}
