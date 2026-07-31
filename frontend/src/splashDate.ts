export type WeekDay = {
  short: string;
  day: number;
  isToday: boolean;
  isCurrentMonth: boolean;
};

export type SeasonId = "winter" | "spring" | "summer" | "autumn";

export type YearMonth = {
  short: string;
  season: SeasonId;
  isCurrent: boolean;
  isPast: boolean;
  isFuture: boolean;
  daysInMonth: number;
  remainingDays: number;
};

export type MonthDay = {
  day: number;
  isToday: boolean;
  isPast: boolean;
  isFuture: boolean;
};

export type SplashDate = {
  weekday: string;
  day: number;
  month: string;
  year: number;
  daysInMonth: number;
  monthDays: MonthDay[];
  isoWeek: number;
  weekDays: WeekDay[];
  yearMonths: YearMonth[];
};

export function getMonthSeason(monthIndex: number): SeasonId {
  if (monthIndex === 11 || monthIndex <= 1) {
    return "winter";
  }
  if (monthIndex <= 4) {
    return "spring";
  }
  if (monthIndex <= 7) {
    return "summer";
  }
  return "autumn";
}

export function getWeekDays(date: Date): WeekDay[] {
  const monday = new Date(date);
  const offset = (date.getDay() + 6) % 7;
  monday.setDate(date.getDate() - offset);

  return Array.from({ length: 7 }, (_, index) => {
    const dayDate = new Date(monday);
    dayDate.setDate(monday.getDate() + index);

    return {
      short: dayDate.toLocaleDateString("en-US", { weekday: "short" }),
      day: dayDate.getDate(),
      isToday: dayDate.toDateString() === date.toDateString(),
      isCurrentMonth: dayDate.getMonth() === date.getMonth(),
    };
  });
}

export function getDaysInMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
}

export function getISOWeek(date: Date) {
  const utc = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = utc.getUTCDay() || 7;
  utc.setUTCDate(utc.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(utc.getUTCFullYear(), 0, 1));
  return Math.ceil((utc.getTime() - yearStart.getTime()) / 86400000 / 7 + 1);
}

export function getYearMonths(date: Date): YearMonth[] {
  const year = date.getFullYear();
  const currentMonth = date.getMonth();
  const currentDay = date.getDate();

  return Array.from({ length: 12 }, (_, monthIndex) => {
    const isCurrent = monthIndex === currentMonth;
    const isPast = monthIndex < currentMonth;
    const isFuture = monthIndex > currentMonth;
    const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
    let remainingDays = 0;
    if (isFuture) {
      remainingDays = daysInMonth;
    } else if (isCurrent) {
      remainingDays = daysInMonth - currentDay + 1;
    }

    return {
      short: new Date(year, monthIndex, 1).toLocaleDateString("en-US", {
        month: "short",
      }),
      season: getMonthSeason(monthIndex),
      isCurrent,
      isPast,
      isFuture,
      daysInMonth,
      remainingDays,
    };
  });
}

export function formatSplashDate(date: Date): SplashDate {
  const day = date.getDate();
  const daysInMonth = getDaysInMonth(date);

  const monthDays = Array.from({ length: daysInMonth }, (_, index) => {
    const dayNumber = index + 1;
    return {
      day: dayNumber,
      isToday: dayNumber === day,
      isPast: dayNumber < day,
      isFuture: dayNumber > day,
    };
  });

  return {
    weekday: date.toLocaleDateString("en-US", { weekday: "long" }),
    day,
    month: date.toLocaleDateString("en-US", { month: "long" }),
    year: date.getFullYear(),
    daysInMonth,
    monthDays,
    isoWeek: getISOWeek(date),
    weekDays: getWeekDays(date),
    yearMonths: getYearMonths(date),
  };
}
