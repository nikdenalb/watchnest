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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PersonalLibraryService {

    private final Clock clock;
    private final ScreenTimeQuotaCalculator quotaCalculator;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final PersonalLibraryStore store;

    public PersonalLibraryService(
            Clock clock,
            ScreenTimeQuotaCalculator quotaCalculator,
            IntegrationEventPublisher integrationEventPublisher,
            PersonalLibraryStore store
    ) {
        this.clock = clock;
        this.quotaCalculator = quotaCalculator;
        this.integrationEventPublisher = integrationEventPublisher;
        this.store = store;
    }

    public DashboardResponse dashboard(UUID ownerId, String username) {
        LibraryProfile profile = store.getOrCreateProfile(ownerId, username);
        List<WatchEvent> ownerEvents = store.findWatchEventsByOwner(ownerId);
        return new DashboardResponse(
                profile.displayName(),
                today(),
                DailyScreenTimeStatusResponse.from(todayStatus(profile, ownerEvents)),
                ScreenTimePolicyResponse.from(profile.screenTimePolicy()),
                todayWatchEvents(ownerEvents).stream().map(WatchEventResponse::from).toList()
        );
    }

    public WatchEventResponse logWatchEvent(UUID ownerId, String username, String contentTitle) {
        if (contentTitle == null || contentTitle.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }

        store.getOrCreateProfile(ownerId, username);

        WatchEvent event = new WatchEvent(
                UUID.randomUUID(),
                ownerId,
                today(),
                contentTitle.trim()
        );
        store.appendWatchEvent(event);
        integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventRecorded(event));
        return WatchEventResponse.from(event);
    }

    public ScreenTimePolicyResponse updateScreenTimePolicy(
            UUID ownerId,
            String username,
            int weekdayEpisodeLimit,
            int weekendEpisodeLimit
    ) {
        LibraryProfile current = store.getOrCreateProfile(ownerId, username);
        ScreenTimePolicy policy = new ScreenTimePolicy(weekdayEpisodeLimit, weekendEpisodeLimit);
        LibraryProfile updated = new LibraryProfile(current.id(), current.displayName(), policy);
        store.saveProfile(updated);
        integrationEventPublisher.publish(
                new PlannerIntegrationEvent.ScreenTimePolicyUpdated(updated.id(), policy)
        );
        return ScreenTimePolicyResponse.from(policy);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private DailyScreenTimeStatus todayStatus(LibraryProfile profile, List<WatchEvent> ownerEvents) {
        return quotaCalculator.summarize(profile, today(), ownerEvents);
    }

    private List<WatchEvent> todayWatchEvents(List<WatchEvent> ownerEvents) {
        LocalDate today = today();
        return ownerEvents.stream()
                .filter(event -> event.watchedOn().equals(today))
                .sorted(Comparator.comparing(WatchEvent::contentTitle))
                .toList();
    }
}
