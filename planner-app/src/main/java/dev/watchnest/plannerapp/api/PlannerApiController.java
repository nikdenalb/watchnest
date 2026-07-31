package dev.watchnest.plannerapp.api;

import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.LogWatchEventRequest;
import dev.watchnest.plannerapp.api.dto.ScreenTimePolicyResponse;
import dev.watchnest.plannerapp.api.dto.UpdateScreenTimePolicyRequest;
import dev.watchnest.plannerapp.api.dto.WatchEventResponse;
import dev.watchnest.plannerapp.library.PersonalLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Planner", description = "Personal watch library API")
public class PlannerApiController {

    private final PersonalLibraryService personalLibraryService;

    public PlannerApiController(PersonalLibraryService personalLibraryService) {
        this.personalLibraryService = personalLibraryService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get today's personal library dashboard")
    public DashboardResponse dashboard() {
        return personalLibraryService.dashboard();
    }

    @PostMapping("/watch-events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Log a watch event for today")
    public WatchEventResponse logWatchEvent(@Valid @RequestBody LogWatchEventRequest request) {
        return personalLibraryService.logWatchEvent(request.contentTitle());
    }

    @PutMapping("/policy")
    @Operation(summary = "Update weekday and weekend screen-time limits")
    public ScreenTimePolicyResponse updatePolicy(@Valid @RequestBody UpdateScreenTimePolicyRequest request) {
        return personalLibraryService.updateScreenTimePolicy(
                request.weekdayEpisodeLimit(),
                request.weekendEpisodeLimit()
        );
    }
}
