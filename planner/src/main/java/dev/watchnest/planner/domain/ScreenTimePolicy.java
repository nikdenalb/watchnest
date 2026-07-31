package dev.watchnest.planner.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public record ScreenTimePolicy(int weekdayEpisodeLimit, int weekendEpisodeLimit) {

    public ScreenTimePolicy {
        if (weekdayEpisodeLimit < 0) {
            throw new IllegalArgumentException("weekdayEpisodeLimit must be non-negative");
        }
        if (weekendEpisodeLimit < 0) {
            throw new IllegalArgumentException("weekendEpisodeLimit must be non-negative");
        }
    }

    public int episodeLimitFor(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return isWeekend(date) ? weekendEpisodeLimit : weekdayEpisodeLimit;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
