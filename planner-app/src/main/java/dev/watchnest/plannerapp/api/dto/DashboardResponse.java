package dev.watchnest.plannerapp.api.dto;

import java.time.LocalDate;

public record DashboardResponse(
        String displayName,
        LocalDate today,
        DailyScreenTimeStatusResponse status,
        ScreenTimePolicyResponse policy,
        PlanTodayResponse planToday,
        boolean treatPlanAsWatched
) {
}
