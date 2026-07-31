package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.DailyScreenTimeStatusResponse;
import dev.watchnest.plannerapp.api.dto.ScreenTimePolicyResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventResponse;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PersonalLibraryService {

    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private final Clock clock;
    private final ScreenTimeQuotaCalculator quotaCalculator;
    private final IntegrationEventPublisher integrationEventPublisher;
    private LibraryProfile libraryProfile;
    private final List<WatchEvent> watchEvents = new ArrayList<>();

    public PersonalLibraryService(
            Clock clock,
            ScreenTimeQuotaCalculator quotaCalculator,
            IntegrationEventPublisher integrationEventPublisher
    ) {
        this.clock = clock;
        this.quotaCalculator = quotaCalculator;
        this.integrationEventPublisher = integrationEventPublisher;
        this.libraryProfile = new LibraryProfile(OWNER_ID, "You", new ScreenTimePolicy(2, 4));
    }

    public DashboardResponse dashboard() {
        var profile = libraryProfile;
        var policy = profile.screenTimePolicy();
        return new DashboardResponse(
                profile.displayName(),
                today(),
                DailyScreenTimeStatusResponse.from(todayStatus()),
                ScreenTimePolicyResponse.from(policy),
                todayWatchEvents().stream().map(WatchEventResponse::from).toList()
        );
    }

    public WatchEventResponse logWatchEvent(String contentTitle) {
        if (contentTitle == null || contentTitle.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }

        WatchEvent event = new WatchEvent(
                UUID.randomUUID(),
                libraryProfile.id(),
                today(),
                contentTitle.trim()
        );
        watchEvents.add(event);
        integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventRecorded(event));
        return WatchEventResponse.from(event);
    }

    public ScreenTimePolicyResponse updateScreenTimePolicy(int weekdayEpisodeLimit, int weekendEpisodeLimit) {
        ScreenTimePolicy policy = new ScreenTimePolicy(weekdayEpisodeLimit, weekendEpisodeLimit);
        libraryProfile = new LibraryProfile(libraryProfile.id(), libraryProfile.displayName(), policy);
        integrationEventPublisher.publish(
                new PlannerIntegrationEvent.ScreenTimePolicyUpdated(libraryProfile.id(), policy)
        );
        return ScreenTimePolicyResponse.from(policy);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public DailyScreenTimeStatus todayStatus() {
        return quotaCalculator.summarize(libraryProfile, today(), watchEvents);
    }

    public List<WatchEvent> todayWatchEvents() {
        LocalDate today = today();
        return watchEvents.stream()
                .filter(event -> event.watchedOn().equals(today))
                .sorted(Comparator.comparing(WatchEvent::contentTitle))
                .toList();
    }
}
