package dev.watchnest.plannerapp.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        String displayName,
        LocalDate today,
        DailyScreenTimeStatusResponse status,
        ScreenTimePolicyResponse policy,
        List<WatchEventResponse> todayEvents
) {
}
