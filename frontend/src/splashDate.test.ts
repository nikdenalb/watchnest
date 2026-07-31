import { describe, expect, it } from "vitest";
import {
  formatSplashDate,
  getDaysInMonth,
  getISOWeek,
  getMonthSeason,
  getWeekDays,
  getYearMonths,
} from "./splashDate";

describe("getMonthSeason", () => {
  it("maps months to seasons", () => {
    expect(getMonthSeason(11)).toBe("winter");
    expect(getMonthSeason(0)).toBe("winter");
    expect(getMonthSeason(1)).toBe("winter");
    expect(getMonthSeason(2)).toBe("spring");
    expect(getMonthSeason(4)).toBe("spring");
    expect(getMonthSeason(5)).toBe("summer");
    expect(getMonthSeason(7)).toBe("summer");
    expect(getMonthSeason(8)).toBe("autumn");
    expect(getMonthSeason(10)).toBe("autumn");
  });
});

describe("getDaysInMonth", () => {
  it("returns day counts including leap February", () => {
    expect(getDaysInMonth(new Date(2026, 6, 15))).toBe(31);
    expect(getDaysInMonth(new Date(2024, 1, 1))).toBe(29);
    expect(getDaysInMonth(new Date(2025, 1, 1))).toBe(28);
  });
});

describe("getISOWeek", () => {
  it("returns known ISO week numbers", () => {
    expect(getISOWeek(new Date(2026, 6, 27))).toBe(31);
    expect(getISOWeek(new Date(2026, 0, 1))).toBe(1);
  });
});

describe("getWeekDays", () => {
  it("builds a Monday-start week with today marked", () => {
    const week = getWeekDays(new Date(2026, 6, 27)); // Monday
    expect(week).toHaveLength(7);
    expect(week[0]?.short).toBe("Mon");
    expect(week[0]?.day).toBe(27);
    expect(week[0]?.isToday).toBe(true);
    expect(week.filter((day) => day.isToday)).toHaveLength(1);
  });
});

describe("getYearMonths", () => {
  it("marks past, current, and future months with remaining days", () => {
    const months = getYearMonths(new Date(2026, 6, 27));
    expect(months).toHaveLength(12);
    expect(months[5]?.isPast).toBe(true);
    expect(months[5]?.remainingDays).toBe(0);
    expect(months[6]?.isCurrent).toBe(true);
    expect(months[6]?.remainingDays).toBe(5);
    expect(months[7]?.isFuture).toBe(true);
    expect(months[7]?.remainingDays).toBe(31);
  });
});

describe("formatSplashDate", () => {
  it("assembles week, month, and year context", () => {
    const splash = formatSplashDate(new Date(2026, 6, 27));
    expect(splash.year).toBe(2026);
    expect(splash.day).toBe(27);
    expect(splash.daysInMonth).toBe(31);
    expect(splash.monthDays).toHaveLength(31);
    expect(splash.monthDays[26]?.isToday).toBe(true);
    expect(splash.weekDays).toHaveLength(7);
    expect(splash.yearMonths).toHaveLength(12);
    expect(splash.isoWeek).toBe(31);
  });
});
