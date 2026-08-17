import { describe, expect, it } from "vitest";
import {
  addDays,
  calendarMonthRange,
  calendarYearRange,
  forwardDisplayRange,
  isoWeekRange,
  mondayOffset,
  planAddTarget,
  shiftForwardAnchor,
} from "./forwardPlanRange";

describe("addDays", () => {
  it("rolls across month and year boundaries", () => {
    expect(addDays("2026-07-31", 1)).toBe("2026-08-01");
    expect(addDays("2026-08-01", -1)).toBe("2026-07-31");
    expect(addDays("2026-01-01", -1)).toBe("2025-12-31");
    expect(addDays("2024-02-28", 1)).toBe("2024-02-29");
    expect(addDays("2025-02-28", 1)).toBe("2025-03-01");
  });
});

describe("isoWeekRange", () => {
  it("uses Monday–Sunday inclusive", () => {
    expect(mondayOffset("1970-01-01")).toBe(3);
    expect(isoWeekRange("2026-07-27")).toEqual({ from: "2026-07-27", to: "2026-08-02" });
    expect(isoWeekRange("2026-07-29")).toEqual({ from: "2026-07-27", to: "2026-08-02" });
    expect(isoWeekRange("2026-08-02")).toEqual({ from: "2026-07-27", to: "2026-08-02" });
    expect(isoWeekRange("2026-01-01")).toEqual({ from: "2025-12-29", to: "2026-01-04" });
    expect(isoWeekRange("2024-01-01")).toEqual({ from: "2024-01-01", to: "2024-01-07" });
  });
});

describe("calendar ranges", () => {
  it("returns the full calendar month, not clipped to today", () => {
    expect(calendarMonthRange("2026-07-27")).toEqual({ from: "2026-07-01", to: "2026-07-31" });
  });

  it("returns the full calendar year including leap 366-day span", () => {
    expect(calendarYearRange("2026-07-27")).toEqual({ from: "2026-01-01", to: "2026-12-31" });
    expect(calendarYearRange("2024-02-29")).toEqual({ from: "2024-01-01", to: "2024-12-31" });
  });
});

describe("forwardDisplayRange", () => {
  it("selects week, month, or year from the same dated collection", () => {
    expect(forwardDisplayRange("2026-07-27", "week")).toEqual({
      from: "2026-07-27",
      to: "2026-08-02",
    });
    expect(forwardDisplayRange("2026-07-27", "month")).toEqual({
      from: "2026-07-01",
      to: "2026-07-31",
    });
    expect(forwardDisplayRange("2026-07-27", "year")).toEqual({
      from: "2026-01-01",
      to: "2026-12-31",
    });
  });
});

describe("shiftForwardAnchor", () => {
  it("moves week by 7 days and clamps month/year day overflow", () => {
    expect(shiftForwardAnchor("2026-07-27", "week", 1)).toBe("2026-08-03");
    expect(shiftForwardAnchor("2026-07-27", "week", -1)).toBe("2026-07-20");
    expect(shiftForwardAnchor("2026-01-31", "month", 1)).toBe("2026-02-28");
    expect(shiftForwardAnchor("2024-02-29", "year", 1)).toBe("2025-02-28");
  });
});

describe("planAddTarget", () => {
  it("routes today to PlanToday, future to forward, and past as disabled", () => {
    expect(planAddTarget("2026-07-27", "2026-07-26")).toBe("past");
    expect(planAddTarget("2026-07-27", "2026-07-27")).toBe("today");
    expect(planAddTarget("2026-07-27", "2026-07-28")).toBe("forward");
  });
});
