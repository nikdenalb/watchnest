package dev.watchnest.planner.domain;

import java.util.Objects;
import java.util.UUID;

public record PlanTodayLine(UUID id, String contentTitle, boolean checked, PlanLineSource source) {

    public PlanTodayLine {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        contentTitle = ContentTitles.requireValid(contentTitle);
    }

    public PlanTodayLine withChecked(boolean checked) {
        return new PlanTodayLine(id, contentTitle, checked, source);
    }
}
