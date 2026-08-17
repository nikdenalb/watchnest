package dev.watchnest.planner.policy;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanTodayLine;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;

public final class ScreenTimeQuotaCalculator {

    public DailyScreenTimeStatus summarize(
            LibraryProfile profile,
            LocalDate date,
            Collection<PlanTodayLine> planLines
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(planLines, "planLines");

        int episodeLimit = profile.screenTimePolicy().episodeLimitFor(date);
        int episodesPlanned = planLines.size();
        int episodesRemaining = Math.max(0, episodeLimit - episodesPlanned);

        return new DailyScreenTimeStatus(date, episodeLimit, episodesPlanned, episodesRemaining);
    }
}
