package dev.watchnest.plannerapp.api;

import dev.watchnest.plannerapp.api.dto.CreateWatchEventRequest;
import dev.watchnest.plannerapp.api.dto.PatchWatchEventRequest;
import dev.watchnest.plannerapp.api.dto.WatchEventArchiveResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventResponse;
import dev.watchnest.plannerapp.auth.WatchNestUser;
import dev.watchnest.plannerapp.library.PersonalLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Planner", description = "Personal watch diary API")
@SecurityRequirement(name = "sessionCookie")
public class PlannerApiController {

    private final PersonalLibraryService personalLibraryService;

    public PlannerApiController(PersonalLibraryService personalLibraryService) {
        this.personalLibraryService = personalLibraryService;
    }

    @GetMapping("/watch-events")
    @Operation(summary = "List diary watch events in an inclusive date range (past, today, and future)")
    public WatchEventArchiveResponse watchEvents(
            @AuthenticationPrincipal WatchNestUser user,
            @Parameter(description = "Inclusive range start (ISO-8601 date)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive range end (ISO-8601 date)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return personalLibraryService.listWatchEvents(user.id(), from, to);
    }

    @PostMapping("/watch-events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a diary watch event (watchedOn may be past, today, or future)")
    public WatchEventResponse addWatchEvent(
            @AuthenticationPrincipal WatchNestUser user,
            @Valid @RequestBody CreateWatchEventRequest request
    ) {
        return personalLibraryService.addWatchEvent(
                user.id(),
                user.getUsername(),
                request.watchedOn(),
                request.contentTitle()
        );
    }

    @PatchMapping("/watch-events/{id}")
    @Operation(summary = "Rename a diary watch event; id and watchedOn are unchanged")
    public WatchEventResponse patchWatchEvent(
            @AuthenticationPrincipal WatchNestUser user,
            @PathVariable UUID id,
            @Valid @RequestBody PatchWatchEventRequest request
    ) {
        return personalLibraryService.patchWatchEvent(user.id(), id, request.contentTitle());
    }

    @DeleteMapping("/watch-events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a diary watch event")
    public void deleteWatchEvent(
            @AuthenticationPrincipal WatchNestUser user,
            @PathVariable UUID id
    ) {
        personalLibraryService.deleteWatchEvent(user.id(), id);
    }
}
