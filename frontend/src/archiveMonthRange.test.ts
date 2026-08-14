import { describe, expect, it } from "vitest";
import {
  addCalendarMonths,
  archiveMonthRange,
  daysInMonth,
  formatDayHeading,
  formatMonthLabel,
  isLeapYear,
  isNextMonthDisabled,
  parseIsoDate,
  yearMonthFromIso,
} from "./archiveMonthRange";

describe("parseIsoDate", () => {
  it("parses ISO components as integers", () => {
    expect(parseIsoDate("2026-08-14")).toEqual({ year: 2026, month: 8, day: 14 });
  });
});

describe("daysInMonth", () => {
  it("returns 29 for leap February and 28 otherwise", () => {
    expect(isLeapYear(2024)).toBe(true);
    expect(daysInMonth(2024, 2)).toBe(29);
    expect(daysInMonth(2025, 2)).toBe(28);
    expect(daysInMonth(2026, 8)).toBe(31);
  });
});

describe("archiveMonthRange", () => {
  it("clips the current month to server today", () => {
    expect(archiveMonthRange("2026-08-14", { year: 2026, month: 8 })).toEqual({
      from: "2026-08-01",
      to: "2026-08-14",
    });
  });

  it("returns the full past month", () => {
    expect(archiveMonthRange("2026-08-14", { year: 2026, month: 7 })).toEqual({
      from: "2026-07-01",
      to: "2026-07-31",
    });
  });

  it("uses 29 days for a past leap February", () => {
    expect(archiveMonthRange("2024-03-01", { year: 2024, month: 2 })).toEqual({
      from: "2024-02-01",
      to: "2024-02-29",
    });
  });

  it("uses 28 days for a past non-leap February", () => {
    expect(archiveMonthRange("2025-03-10", { year: 2025, month: 2 })).toEqual({
      from: "2025-02-01",
      to: "2025-02-28",
    });
  });
});

describe("addCalendarMonths", () => {
  it("rolls December to January of the next year", () => {
    expect(addCalendarMonths({ year: 2026, month: 12 }, 1)).toEqual({ year: 2027, month: 1 });
  });

  it("rolls January to December of the previous year", () => {
    expect(addCalendarMonths({ year: 2026, month: 1 }, -1)).toEqual({ year: 2025, month: 12 });
  });
});

describe("isNextMonthDisabled", () => {
  it("disables next when that month starts after today", () => {
    expect(isNextMonthDisabled("2026-08-14", { year: 2026, month: 8 })).toBe(true);
    expect(isNextMonthDisabled("2026-08-14", { year: 2026, month: 7 })).toBe(false);
    expect(isNextMonthDisabled("2026-12-31", { year: 2026, month: 12 })).toBe(true);
  });
});

describe("labels", () => {
  it("formats month and day headings from integer parts", () => {
    expect(formatMonthLabel(yearMonthFromIso("2026-08-14"))).toBe("August 2026");
    expect(formatDayHeading("2026-08-14")).toBe("14 Aug");
  });
});
