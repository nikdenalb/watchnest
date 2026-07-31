package dev.watchnest.plannerapp.api.dto;

import dev.watchnest.planner.domain.WatchEvent;

import java.time.LocalDate;
import java.util.UUID;

public record WatchEventResponse(UUID id, UUID ownerId, LocalDate watchedOn, String contentTitle) {

    public static WatchEventResponse from(WatchEvent event) {
        return new WatchEventResponse(event.id(), event.ownerId(), event.watchedOn(), event.contentTitle());
    }
}
