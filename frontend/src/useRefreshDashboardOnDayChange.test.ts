import { describe, expect, it } from "vitest";
import { localDateIso, msUntilNextLocalMidnight } from "./useRefreshDashboardOnDayChange";

describe("localDateIso", () => {
  it("formats a local calendar date as YYYY-MM-DD", () => {
    expect(localDateIso(new Date(2026, 6, 27, 23, 59, 59))).toBe("2026-07-27");
    expect(localDateIso(new Date(2026, 0, 5))).toBe("2026-01-05");
  });
});

describe("msUntilNextLocalMidnight", () => {
  it("returns milliseconds until the next local midnight", () => {
    const now = new Date(2026, 6, 27, 22, 0, 0);
    const nextMidnight = new Date(2026, 6, 28, 0, 0, 0);
    expect(msUntilNextLocalMidnight(now)).toBe(nextMidnight.getTime() - now.getTime());
  });

  it("never returns zero or negative", () => {
    expect(msUntilNextLocalMidnight(new Date(2026, 6, 27, 23, 59, 59, 999))).toBeGreaterThan(0);
  });
});
