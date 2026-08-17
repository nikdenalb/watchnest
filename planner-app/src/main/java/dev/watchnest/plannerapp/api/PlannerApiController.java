package dev.watchnest.plannerapp.api;

import dev.watchnest.plannerapp.api.dto.ContentTitleRequest;
import dev.watchnest.plannerapp.api.dto.CreateForwardPlanItemRequest;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.ForwardPlanItemResponse;
import dev.watchnest.plannerapp.api.dto.ForwardPlanResponse;
import dev.watchnest.plannerapp.api.dto.PatchPlanTodayLineRequest;
import dev.watchnest.plannerapp.api.dto.PlanTodayLineResponse;
import dev.watchnest.plannerapp.api.dto.ScreenTimePolicyResponse;
import dev.watchnest.plannerapp.api.dto.UpdateScreenTimePolicyRequest;
import dev.watchnest.plannerapp.api.dto.WatchEventArchiveResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

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
        return personalLibraryService.watchEventArchive(user.id(), user.getUsername(), from, to);
    }

    @PostMapping("/plan/today/lines")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a PlanToday line for today")
    public PlanTodayLineResponse addPlanTodayLine(
            @AuthenticationPrincipal WatchNestUser user,
            @Valid @RequestBody ContentTitleRequest request
    ) {
        return personalLibraryService.addPlanTodayLine(user.id(), user.getUsername(), request.contentTitle());
    }

    @PatchMapping("/plan/today/lines/{id}")
    @Operation(summary = "Set checked on a PlanToday line")
    public PlanTodayLineResponse patchPlanTodayLine(
            @AuthenticationPrincipal WatchNestUser user,
            @PathVariable UUID id,
            @Valid @RequestBody PatchPlanTodayLineRequest request
    ) {
        return personalLibraryService.patchPlanTodayLine(
                user.id(),
                user.getUsername(),
                id,
                request.checked()
        );
    }

    @DeleteMapping("/plan/today/lines/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a PlanToday line")
    public void deletePlanTodayLine(
            @AuthenticationPrincipal WatchNestUser user,
            @PathVariable UUID id
    ) {
        personalLibraryService.deletePlanTodayLine(user.id(), user.getUsername(), id);
    }

    @GetMapping("/plan/forward")
    @Operation(summary = "List dated forward-plan items in a date range")
    public ForwardPlanResponse forwardPlan(
            @AuthenticationPrincipal WatchNestUser user,
            @Parameter(description = "Inclusive range start (ISO-8601 date)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive range end (ISO-8601 date)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return personalLibraryService.forwardPlan(user.id(), user.getUsername(), from, to);
    }

    @PostMapping("/plan/forward")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a dated forward-plan item (plannedFor after today)")
    public ForwardPlanItemResponse addForwardPlanItem(
            @AuthenticationPrincipal WatchNestUser user,
            @Valid @RequestBody CreateForwardPlanItemRequest request
    ) {
        return personalLibraryService.addForwardPlanItem(
                user.id(),
                user.getUsername(),
                request.plannedFor(),
                request.contentTitle()
        );
    }

    @DeleteMapping("/plan/forward/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a dated forward-plan item")
    public void deleteForwardPlanItem(
            @AuthenticationPrincipal WatchNestUser user,
            @PathVariable UUID id
    ) {
        personalLibraryService.deleteForwardPlanItem(user.id(), user.getUsername(), id);
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
