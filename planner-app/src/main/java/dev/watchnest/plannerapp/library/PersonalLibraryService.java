package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.DailyScreenTimeStatus;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.DailyScreenTimeStatusResponse;
import dev.watchnest.plannerapp.api.dto.ScreenTimePolicyResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventArchiveResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventResponse;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PersonalLibraryService {

    public static final int MAX_ARCHIVE_RANGE_DAYS = 366;

    private final Clock clock;
    private final ScreenTimeQuotaCalculator quotaCalculator;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final PersonalLibraryStore store;
    private final ObjectProvider<PlatformTransactionManager> transactionManagers;

    public PersonalLibraryService(
            Clock clock,
            ScreenTimeQuotaCalculator quotaCalculator,
            IntegrationEventPublisher integrationEventPublisher,
            PersonalLibraryStore store,
            ObjectProvider<PlatformTransactionManager> transactionManagers
    ) {
        this.clock = clock;
        this.quotaCalculator = quotaCalculator;
        this.integrationEventPublisher = integrationEventPublisher;
        this.store = store;
        this.transactionManagers = transactionManagers;
    }

    public DashboardResponse dashboard(UUID ownerId, String username) {
        LocalDate currentDate = today();
        LibraryProfile profile = store.getOrCreateProfile(ownerId, username);
        List<WatchEvent> todayEvents = store.findWatchEventsByOwnerAndWatchedOnBetween(
                ownerId,
                currentDate,
                currentDate
        );
        return new DashboardResponse(
                profile.displayName(),
                currentDate,
                DailyScreenTimeStatusResponse.from(todayStatus(profile, currentDate, todayEvents)),
                ScreenTimePolicyResponse.from(profile.screenTimePolicy()),
                todayEvents.stream().map(WatchEventResponse::from).toList()
        );
    }

    public WatchEventArchiveResponse watchEventArchive(UUID ownerId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(ownerId, "ownerId");
        requireValidArchiveRange(from, to);
        List<WatchEventResponse> events = store.findWatchEventsByOwnerAndWatchedOnBetween(ownerId, from, to)
                .stream()
                .map(WatchEventResponse::from)
                .toList();
        return new WatchEventArchiveResponse(from, to, events);
    }

    public WatchEventResponse logWatchEvent(UUID ownerId, String username, String contentTitle) {
        return inWriteTransaction(() -> doLogWatchEvent(ownerId, username, contentTitle));
    }

    public ScreenTimePolicyResponse updateScreenTimePolicy(
            UUID ownerId,
            String username,
            int weekdayEpisodeLimit,
            int weekendEpisodeLimit
    ) {
        return inWriteTransaction(() -> doUpdatePolicy(ownerId, username, weekdayEpisodeLimit, weekendEpisodeLimit));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    static void requireValidArchiveRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (inclusiveDays > MAX_ARCHIVE_RANGE_DAYS) {
            throw new IllegalArgumentException("archive range must be at most 366 days");
        }
    }

    private WatchEventResponse doLogWatchEvent(UUID ownerId, String username, String contentTitle) {
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

    private ScreenTimePolicyResponse doUpdatePolicy(
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

    private <T> T inWriteTransaction(Supplier<T> action) {
        PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (transactionManager == null) {
            return action.get();
        }
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private DailyScreenTimeStatus todayStatus(
            LibraryProfile profile,
            LocalDate currentDate,
            List<WatchEvent> ownerEvents
    ) {
        return quotaCalculator.summarize(profile, currentDate, ownerEvents);
    }
}
