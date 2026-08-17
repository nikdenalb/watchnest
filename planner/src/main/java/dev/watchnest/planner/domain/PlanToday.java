package dev.watchnest.planner.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlanToday(UUID ownerId, LocalDate forDate, List<PlanTodayLine> lines) {

    public PlanToday {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(forDate, "forDate");
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }

    public static PlanToday empty(UUID ownerId, LocalDate forDate) {
        return new PlanToday(ownerId, forDate, List.of());
    }

    public PlanToday append(PlanTodayLine line) {
        Objects.requireNonNull(line, "line");
        List<PlanTodayLine> next = new ArrayList<>(lines);
        next.add(line);
        return new PlanToday(ownerId, forDate, next);
    }

    public PlanToday removeLine(UUID lineId) {
        Objects.requireNonNull(lineId, "lineId");
        List<PlanTodayLine> next = new ArrayList<>(lines.size());
        for (PlanTodayLine line : lines) {
            if (!line.id().equals(lineId)) {
                next.add(line);
            }
        }
        return new PlanToday(ownerId, forDate, next);
    }

    public PlanToday withLineChecked(UUID lineId, boolean checked) {
        Objects.requireNonNull(lineId, "lineId");
        List<PlanTodayLine> next = new ArrayList<>(lines.size());
        boolean found = false;
        for (PlanTodayLine line : lines) {
            if (line.id().equals(lineId)) {
                next.add(line.withChecked(checked));
                found = true;
            } else {
                next.add(line);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("unknown PlanToday line: " + lineId);
        }
        return new PlanToday(ownerId, forDate, next);
    }
}
