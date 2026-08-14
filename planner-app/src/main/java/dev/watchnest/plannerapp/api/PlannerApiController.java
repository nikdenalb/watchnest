package dev.watchnest.plannerapp.api;

import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.LogWatchEventRequest;
import dev.watchnest.plannerapp.api.dto.ScreenTimePolicyResponse;
import dev.watchnest.plannerapp.api.dto.UpdateScreenTimePolicyRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Planner", description = "Personal watch library API")
@SecurityRequirement(name = "sessionCookie")
public class PlannerApiController {

    private final PersonalLibraryService personalLibraryService;

    public PlannerApiController(PersonalLibraryService personalLibraryService) {
        this.personalLibraryService = personalLibraryService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get today's personal library dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal WatchNestUser user) {
        return personalLibraryService.dashboard(user.id(), user.getUsername());
    }

    @GetMapping("/watch-events")
    @Operation(summary = "List watch events in a date range")
    public WatchEventArchiveResponse watchEvents(
            @AuthenticationPrincipal WatchNestUser user,
            @Parameter(description = "Inclusive range start (ISO-8601 date)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive range end (ISO-8601 date)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return personalLibraryService.watchEventArchive(user.id(), from, to);
    }

    @PostMapping("/watch-events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Log a watch event for today")
    public WatchEventResponse logWatchEvent(
            @AuthenticationPrincipal WatchNestUser user,
            @Valid @RequestBody LogWatchEventRequest request
    ) {
        return personalLibraryService.logWatchEvent(user.id(), user.getUsername(), request.contentTitle());
    }

    @PutMapping("/policy")
    @Operation(summary = "Update weekday and weekend screen-time limits")
    public ScreenTimePolicyResponse updatePolicy(
            @AuthenticationPrincipal WatchNestUser user,
            @Valid @RequestBody UpdateScreenTimePolicyRequest request
    ) {
        return personalLibraryService.updateScreenTimePolicy(
                user.id(),
                user.getUsername(),
                request.weekdayEpisodeLimit(),
                request.weekendEpisodeLimit()
        );
    }
}
