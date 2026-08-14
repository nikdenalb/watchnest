import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, act } from "@testing-library/react";
import { createElement, type ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useRefreshDashboardOnDayChange } from "./useRefreshDashboardOnDayChange";

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe("useRefreshDashboardOnDayChange", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("does nothing without an anchored today", () => {
    const queryClient = new QueryClient();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    renderHook(() => useRefreshDashboardOnDayChange(undefined), {
      wrapper: createWrapper(queryClient),
    });

    act(() => {
      window.dispatchEvent(new Event("focus"));
    });

    expect(invalidate).not.toHaveBeenCalled();
  });

  it("invalidates dashboard when local day no longer matches the anchor", () => {
    vi.setSystemTime(new Date(2026, 6, 27, 12, 0, 0));
    const queryClient = new QueryClient();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    renderHook(() => useRefreshDashboardOnDayChange("2026-07-27"), {
      wrapper: createWrapper(queryClient),
    });

    act(() => {
      window.dispatchEvent(new Event("focus"));
    });
    expect(invalidate).not.toHaveBeenCalled();

    vi.setSystemTime(new Date(2026, 6, 28, 0, 1, 0));
    act(() => {
      window.dispatchEvent(new Event("focus"));
    });

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] });
    expect(
      invalidate.mock.calls.some((call) => call[0]?.queryKey?.[0] === "watch-events"),
    ).toBe(false);
  });

  it("invalidates at local midnight", () => {
    vi.setSystemTime(new Date(2026, 6, 27, 23, 59, 0));
    const queryClient = new QueryClient();
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");

    renderHook(() => useRefreshDashboardOnDayChange("2026-07-27"), {
      wrapper: createWrapper(queryClient),
    });

    act(() => {
      vi.advanceTimersByTime(60_000);
    });

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["dashboard"] });
    expect(
      invalidate.mock.calls.some((call) => call[0]?.queryKey?.[0] === "watch-events"),
    ).toBe(false);
  });
});
