package dev.watchnest.planner.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record ForwardPlanItem(UUID id, UUID ownerId, LocalDate plannedFor, String contentTitle) {

    public ForwardPlanItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(plannedFor, "plannedFor");
        contentTitle = ContentTitles.requireValid(contentTitle);
    }
}
