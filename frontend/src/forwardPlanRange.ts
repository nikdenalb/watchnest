import { addCalendarMonths, daysInMonth, isLeapYear, parseIsoDate, toIsoDate, yearMonthFromIso } from "./archiveMonthRange";

export type ForwardDisplayMode = "week" | "month" | "year";

export type DateRange = {
  from: string;
  to: string;
};

export function addDays(iso: string, delta: number): string {
  let { year, month, day } = parseIsoDate(iso);
  day += delta;
  while (day > daysInMonth(year, month)) {
    day -= daysInMonth(year, month);
    month += 1;
    if (month > 12) {
      month = 1;
      year += 1;
    }
  }
  while (day < 1) {
    month -= 1;
    if (month < 1) {
      month = 12;
      year -= 1;
    }
    day += daysInMonth(year, month);
  }
  return toIsoDate(year, month, day);
}

function daysSinceUnixEpoch(iso: string): number {
  const { year, month, day } = parseIsoDate(iso);
  let days = 0;
  for (let y = 1970; y < year; y += 1) {
    days += isLeapYear(y) ? 366 : 365;
  }
  for (let m = 1; m < month; m += 1) {
    days += daysInMonth(year, m);
  }
  return days + day - 1;
}

/** Monday = 0 … Sunday = 6, from integer ISO parts (1970-01-01 is Thursday). */
export function mondayOffset(iso: string): number {
  return (daysSinceUnixEpoch(iso) + 3) % 7;
}

export function isoWeekRange(iso: string): DateRange {
  const from = addDays(iso, -mondayOffset(iso));
  return { from, to: addDays(from, 6) };
}

export function calendarMonthRange(iso: string): DateRange {
  const { year, month } = parseIsoDate(iso);
  return {
    from: toIsoDate(year, month, 1),
    to: toIsoDate(year, month, daysInMonth(year, month)),
  };
}

export function calendarYearRange(iso: string): DateRange {
  const { year } = parseIsoDate(iso);
  return {
    from: toIsoDate(year, 1, 1),
    to: toIsoDate(year, 12, 31),
  };
}

export function forwardDisplayRange(iso: string, mode: ForwardDisplayMode): DateRange {
  if (mode === "week") {
    return isoWeekRange(iso);
  }
  if (mode === "month") {
    return calendarMonthRange(iso);
  }
  return calendarYearRange(iso);
}

export function shiftForwardAnchor(iso: string, mode: ForwardDisplayMode, delta: number): string {
  if (mode === "week") {
    return addDays(iso, delta * 7);
  }
  if (mode === "month") {
    const shifted = addCalendarMonths(yearMonthFromIso(iso), delta);
    const { day } = parseIsoDate(iso);
    const last = daysInMonth(shifted.year, shifted.month);
    return toIsoDate(shifted.year, shifted.month, Math.min(day, last));
  }
  const { year, month, day } = parseIsoDate(iso);
  const nextYear = year + delta;
  const last = daysInMonth(nextYear, month);
  return toIsoDate(nextYear, month, Math.min(day, last));
}

export function planAddTarget(today: string, plannedFor: string): "past" | "today" | "forward" {
  if (plannedFor < today) {
    return "past";
  }
  if (plannedFor === today) {
    return "today";
  }
  return "forward";
}
