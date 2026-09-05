export type CalendarMonth = {
  year: number;
  month: number;
};

export type IsoDateParts = {
  year: number;
  month: number;
  day: number;
};

const MONTH_LENGTHS = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

const MONTH_NAMES = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];

const MONTH_SHORT = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

export function parseIsoDate(iso: string): IsoDateParts {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!match) {
    throw new Error(`Invalid ISO date: ${iso}`);
  }
  return {
    year: Number.parseInt(match[1], 10),
    month: Number.parseInt(match[2], 10),
    day: Number.parseInt(match[3], 10),
  };
}

function pad2(value: number): string {
  return String(value).padStart(2, "0");
}

export function toIsoDate(year: number, month: number, day: number): string {
  return `${year}-${pad2(month)}-${pad2(day)}`;
}

export function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

export function daysInMonth(year: number, month: number): number {
  if (month === 2) {
    return isLeapYear(year) ? 29 : 28;
  }
  return MONTH_LENGTHS[month - 1] ?? 0;
}

/** Local calendar date as `YYYY-MM-DD` (matches API `LocalDate` JSON). */
export function localDateIso(date = new Date()): string {
  return toIsoDate(date.getFullYear(), date.getMonth() + 1, date.getDate());
}

export function yearMonthFromIso(iso: string): CalendarMonth {
  const { year, month } = parseIsoDate(iso);
  return { year, month };
}

export function addCalendarMonths(selected: CalendarMonth, delta: number): CalendarMonth {
  const index = selected.year * 12 + (selected.month - 1) + delta;
  const year = Math.floor(index / 12);
  const monthIndex = ((index % 12) + 12) % 12;
  return { year, month: monthIndex + 1 };
}

export function calendarMonthRange(selected: CalendarMonth): { from: string; to: string } {
  return {
    from: toIsoDate(selected.year, selected.month, 1),
    to: toIsoDate(selected.year, selected.month, daysInMonth(selected.year, selected.month)),
  };
}

export function formatMonthLabel(selected: CalendarMonth): string {
  return `${MONTH_NAMES[selected.month - 1]} ${selected.year}`;
}

export function formatDayHeading(iso: string): string {
  const { day, month } = parseIsoDate(iso);
  return `${day} ${MONTH_SHORT[month - 1]}`;
}
