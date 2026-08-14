package dev.watchnest.plannerapp.api.dto;

import java.time.LocalDate;
import java.util.List;

public record WatchEventArchiveResponse(
        LocalDate from,
        LocalDate to,
        List<WatchEventResponse> events
) {
}
