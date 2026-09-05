import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SplashScreen, SPLASH_SKIP_HINT_MIN_MS } from "./SplashScreen";

describe("SplashScreen", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    act(() => {
      vi.runOnlyPendingTimers();
    });
    cleanup();
    vi.useRealTimers();
  });

  it("does not allow skip while the app is loading", () => {
    const onDismiss = vi.fn();
    render(<SplashScreen appReady={false} onDismiss={onDismiss} />);

    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(screen.getByText("Press any key or click to continue")).not.toHaveClass("is-visible");

    fireEvent.keyDown(window, { key: "Enter" });
    expect(onDismiss).not.toHaveBeenCalled();
  });

  it("allows skip as soon as the app is ready, before the hint appears", () => {
    const onDismiss = vi.fn();
    render(<SplashScreen appReady onDismiss={onDismiss} />);

    expect(screen.getByRole("button")).toBeInTheDocument();
    expect(screen.getByText("Press any key or click to continue")).not.toHaveClass("is-visible");

    fireEvent.keyDown(window, { key: "Enter" });
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("allows skip by click when the app is ready", () => {
    const onDismiss = vi.fn();
    render(<SplashScreen appReady onDismiss={onDismiss} />);

    fireEvent.click(screen.getByRole("button"));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("does not dismiss on click while the app is loading", () => {
    const onDismiss = vi.fn();
    render(<SplashScreen appReady={false} onDismiss={onDismiss} />);

    fireEvent.click(screen.getByRole("status"));
    expect(onDismiss).not.toHaveBeenCalled();
  });

  it("shows the skip hint only after ready and the minimum delay", () => {
    const onDismiss = vi.fn();
    render(<SplashScreen appReady onDismiss={onDismiss} />);

    const hint = screen.getByText("Press any key or click to continue");
    expect(hint).not.toHaveClass("is-visible");

    act(() => {
      vi.advanceTimersByTime(SPLASH_SKIP_HINT_MIN_MS);
    });

    expect(hint).toHaveClass("is-visible");
  });

  it("does not show the hint after the delay if the app is still loading", () => {
    render(<SplashScreen appReady={false} onDismiss={vi.fn()} />);

    act(() => {
      vi.advanceTimersByTime(SPLASH_SKIP_HINT_MIN_MS);
    });

    expect(screen.getByText("Press any key or click to continue")).not.toHaveClass("is-visible");
  });

  it("renders calendar context labels", () => {
    render(<SplashScreen appReady={false} onDismiss={vi.fn()} />);

    expect(screen.getByText("WatchNest")).toBeInTheDocument();
    expect(screen.getByLabelText("Watch Flow Diary")).toBeInTheDocument();
    expect(screen.getByText("Week")).toBeInTheDocument();
    expect(screen.getByText("Month")).toBeInTheDocument();
    expect(screen.getByText("Year")).toBeInTheDocument();
  });
});
