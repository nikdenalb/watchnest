package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.ForwardPlanItem;
import dev.watchnest.planner.domain.LibraryLimits;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanLineSource;
import dev.watchnest.planner.domain.PlanToday;
import dev.watchnest.planner.domain.PlanTodayLine;
import dev.watchnest.planner.domain.ScreenTimePolicy;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.DailyScreenTimeStatusResponse;
import dev.watchnest.plannerapp.api.dto.ForwardPlanItemResponse;
import dev.watchnest.plannerapp.api.dto.ForwardPlanResponse;
import dev.watchnest.plannerapp.api.dto.LibraryPreferencesResponse;
import dev.watchnest.plannerapp.api.dto.PlanTodayLineResponse;
import dev.watchnest.plannerapp.api.dto.PlanTodayResponse;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class PersonalLibraryService {

    public static final int MAX_ARCHIVE_RANGE_DAYS = 366;
    public static final int MAX_ARCHIVE_TITLE_LENGTH = 120;

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
        Objects.requireNonNull(ownerId, "ownerId");
        return inOwnerWrite(ownerId, () -> {
            PlanToday plan = ensurePlanToday(ownerId, username);
            LibraryProfile profile = store.getOrCreateProfile(ownerId, username);
            return new DashboardResponse(
                    profile.displayName(),
                    plan.forDate(),
                    DailyScreenTimeStatusResponse.from(
                            quotaCalculator.summarize(profile, plan.forDate(), plan.lines())
                    ),
                    ScreenTimePolicyResponse.from(profile.screenTimePolicy()),
                    PlanTodayResponse.from(plan),
                    profile.treatPlanAsWatched()
            );
        });
    }

    public WatchEventArchiveResponse watchEventArchive(
            UUID ownerId,
            String username,
            LocalDate from,
            LocalDate to
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        requireValidInclusiveDateRange(from, to);
        return inOwnerWrite(ownerId, () -> {
            ensurePlanToday(ownerId, username);
            List<WatchEventResponse> events = store.findWatchEventsByOwnerAndWatchedOnBetween(ownerId, from, to)
                    .stream()
                    .map(WatchEventResponse::from)
                    .toList();
            return new WatchEventArchiveResponse(from, to, events);
        });
    }

    public WatchEventResponse addWatchEvent(
            UUID ownerId,
            String username,
            LocalDate watchedOn,
            String contentTitle
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        LocalDate currentDate = today();
        String title = normalizeArchiveTitle(contentTitle);
        if (watchedOn == null) {
            throw new IllegalArgumentException("watchedOn is required");
        }
        if (!watchedOn.isBefore(currentDate)) {
            throw new IllegalArgumentException("watchedOn must be before today");
        }
        return inOwnerWrite(ownerId, () -> {
            boolean treatPlanAsWatched = store.getOrCreateProfile(ownerId, username).treatPlanAsWatched();
            int extra = projectedEnsureArchiveWrites(ownerId, currentDate, watchedOn, treatPlanAsWatched);
            if (store.countWatchEventsByOwnerAndWatchedOn(ownerId, watchedOn) + extra
                    >= LibraryLimits.MAX_TITLES_PER_DATE) {
                throw new IllegalArgumentException(
                        "a day may have at most " + LibraryLimits.MAX_TITLES_PER_DATE + " watch events"
                );
            }
            ensurePlanToday(ownerId, username, treatPlanAsWatched);
            WatchEvent event = new WatchEvent(UUID.randomUUID(), ownerId, watchedOn, title);
            store.appendWatchEvent(event);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventRecorded(event));
            return WatchEventResponse.from(event);
        });
    }

    public WatchEventResponse patchWatchEvent(UUID ownerId, String username, UUID id, String contentTitle) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        String title = normalizeArchiveTitle(contentTitle);
        return inOwnerWrite(ownerId, () -> {
            LocalDate currentDate = today();
            WatchEvent current = store.findWatchEventByOwnerAndId(ownerId, id)
                    .orElseThrow(() -> new PlanResourceNotFoundException("watch event not found"));
            if (!current.watchedOn().isBefore(currentDate)) {
                throw new IllegalArgumentException("watchedOn must be before today");
            }
            ensurePlanToday(ownerId, username);
            if (title.equals(current.contentTitle())) {
                return WatchEventResponse.from(current);
            }
            store.updateWatchEventTitle(ownerId, id, title);
            WatchEvent updated = new WatchEvent(current.id(), current.ownerId(), current.watchedOn(), title);
            integrationEventPublisher.publish(
                    new PlannerIntegrationEvent.WatchEventCorrected(current.contentTitle(), updated)
            );
            return WatchEventResponse.from(updated);
        });
    }

    public void deleteWatchEvent(UUID ownerId, String username, UUID id) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(id, "id");
        inOwnerWrite(ownerId, () -> {
            LocalDate currentDate = today();
            WatchEvent current = store.findWatchEventByOwnerAndId(ownerId, id)
                    .orElseThrow(() -> new PlanResourceNotFoundException("watch event not found"));
            if (!current.watchedOn().isBefore(currentDate)) {
                throw new IllegalArgumentException("watchedOn must be before today");
            }
            ensurePlanToday(ownerId, username);
            store.deleteWatchEvent(ownerId, id);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventDeleted(
                    current.ownerId(),
                    current.id(),
                    current.watchedOn(),
                    current.contentTitle()
            ));
            return null;
        });
    }

    public ForwardPlanResponse forwardPlan(UUID ownerId, String username, LocalDate from, LocalDate to) {
        Objects.requireNonNull(ownerId, "ownerId");
        requireValidInclusiveDateRange(from, to);
        return inOwnerWrite(ownerId, () -> {
            ensurePlanToday(ownerId, username);
            List<ForwardPlanItemResponse> items = store
                    .findForwardPlanItemsByOwnerAndPlannedForBetween(ownerId, from, to)
                    .stream()
                    .map(ForwardPlanItemResponse::from)
                    .toList();
            return new ForwardPlanResponse(from, to, items);
        });
    }

    public PlanTodayLineResponse addPlanTodayLine(UUID ownerId, String username, String contentTitle) {
        requireContentTitle(contentTitle);
        return inOwnerWrite(ownerId, () -> {
            PlanToday plan = ensurePlanToday(ownerId, username);
            if (plan.lines().size() >= LibraryLimits.MAX_TITLES_PER_DATE) {
                throw new IllegalArgumentException(
                        "PlanToday may have at most " + LibraryLimits.MAX_TITLES_PER_DATE + " lines"
                );
            }
            boolean checked = store.getOrCreateProfile(ownerId, username).treatPlanAsWatched();
            PlanTodayLine line = new PlanTodayLine(
                    UUID.randomUUID(),
                    contentTitle,
                    checked,
                    PlanLineSource.MANUAL
            );
            store.savePlanToday(plan.append(line));
            return PlanTodayLineResponse.from(line);
        });
    }

    public PlanTodayLineResponse patchPlanTodayLine(
            UUID ownerId,
            String username,
            UUID lineId,
            boolean checked
    ) {
        Objects.requireNonNull(lineId, "lineId");
        return inOwnerWrite(ownerId, () -> {
            if (store.getOrCreateProfile(ownerId, username).treatPlanAsWatched()) {
                throw new IllegalArgumentException("checked cannot be changed when treatPlanAsWatched is enabled");
            }
            PlanToday plan = ensurePlanToday(ownerId, username);
            requireLine(plan, lineId);
            PlanToday updated = plan.withLineChecked(lineId, checked);
            store.savePlanToday(updated);
            PlanTodayLine line = updated.lines().stream()
                    .filter(item -> item.id().equals(lineId))
                    .findFirst()
                    .orElseThrow();
            return PlanTodayLineResponse.from(line);
        });
    }

    public void deletePlanTodayLine(UUID ownerId, String username, UUID lineId) {
        Objects.requireNonNull(lineId, "lineId");
        inOwnerWrite(ownerId, () -> {
            PlanToday plan = ensurePlanToday(ownerId, username);
            requireLine(plan, lineId);
            store.savePlanToday(plan.removeLine(lineId));
            return null;
        });
    }

    public ForwardPlanItemResponse addForwardPlanItem(
            UUID ownerId,
            String username,
            LocalDate plannedFor,
            String contentTitle
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        if (plannedFor == null) {
            throw new IllegalArgumentException("plannedFor is required");
        }
        requireContentTitle(contentTitle);
        ForwardPlanItem item = new ForwardPlanItem(UUID.randomUUID(), ownerId, plannedFor, contentTitle);
        return inOwnerWrite(ownerId, () -> {
            LocalDate currentDate = today();
            if (!plannedFor.isAfter(currentDate)) {
                throw new IllegalArgumentException("plannedFor must be after today");
            }
            ensurePlanToday(ownerId, username);
            if (store.countForwardPlanItemsByOwnerAndPlannedFor(ownerId, plannedFor)
                    >= LibraryLimits.MAX_TITLES_PER_DATE) {
                throw new IllegalArgumentException(
                        "forward plan may have at most " + LibraryLimits.MAX_TITLES_PER_DATE + " items per date"
                );
            }
            store.appendForwardPlanItem(item);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.ForwardPlanItemAdded(item));
            return ForwardPlanItemResponse.from(item);
        });
    }

    public void deleteForwardPlanItem(UUID ownerId, String username, UUID itemId) {
        Objects.requireNonNull(itemId, "itemId");
        inOwnerWrite(ownerId, () -> {
            ensurePlanToday(ownerId, username);
            ForwardPlanItem item = store.findForwardPlanItemByOwnerAndId(ownerId, itemId)
                    .orElseThrow(() -> new PlanResourceNotFoundException("forward plan item not found"));
            store.deleteForwardPlanItem(ownerId, itemId);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.ForwardPlanItemRemoved(
                    ownerId,
                    item.id(),
                    item.plannedFor(),
                    item.contentTitle(),
                    PlannerIntegrationEvent.ForwardPlanItemRemovalReason.USER_DELETED
            ));
            return null;
        });
    }

    public ScreenTimePolicyResponse updateScreenTimePolicy(
            UUID ownerId,
            String username,
            int weekdayEpisodeLimit,
            int weekendEpisodeLimit
    ) {
        ScreenTimePolicy policy = new ScreenTimePolicy(weekdayEpisodeLimit, weekendEpisodeLimit);
        return inOwnerWrite(ownerId, () -> {
            ensurePlanToday(ownerId, username);
            LibraryProfile current = store.getOrCreateProfile(ownerId, username);
            LibraryProfile updated = new LibraryProfile(
                    current.id(),
                    current.displayName(),
                    policy,
                    current.treatPlanAsWatched()
            );
            store.saveProfile(updated);
            integrationEventPublisher.publish(
                    new PlannerIntegrationEvent.ScreenTimePolicyUpdated(updated.id(), policy)
            );
            return ScreenTimePolicyResponse.from(policy);
        });
    }

    public LibraryPreferencesResponse updateLibraryPreferences(
            UUID ownerId,
            String username,
            Boolean treatPlanAsWatched
    ) {
        if (treatPlanAsWatched == null) {
            throw new IllegalArgumentException("treatPlanAsWatched is required");
        }
        return inOwnerWrite(ownerId, () -> {
            LibraryProfile current = store.getOrCreateProfile(ownerId, username);
            preflightEnsure(ownerId);
            boolean changed = current.treatPlanAsWatched() != treatPlanAsWatched;
            if (changed) {
                store.saveProfile(new LibraryProfile(
                        current.id(),
                        current.displayName(),
                        current.screenTimePolicy(),
                        treatPlanAsWatched
                ));
            }
            PlanToday plan = ensurePlanToday(ownerId, username, treatPlanAsWatched);
            if (treatPlanAsWatched) {
                checkAllPlanTodayLines(plan);
            }
            if (changed) {
                integrationEventPublisher.publish(
                        new PlannerIntegrationEvent.LibraryPreferencesUpdated(ownerId, treatPlanAsWatched)
                );
            }
            return new LibraryPreferencesResponse(treatPlanAsWatched);
        });
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    static String normalizeArchiveTitle(String contentTitle) {
        if (contentTitle == null) {
            throw new IllegalArgumentException("contentTitle is required");
        }
        String trimmed = contentTitle.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }
        if (trimmed.length() > MAX_ARCHIVE_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "contentTitle must be at most " + MAX_ARCHIVE_TITLE_LENGTH + " characters"
            );
        }
        return trimmed;
    }

    static void requireValidInclusiveDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (inclusiveDays > MAX_ARCHIVE_RANGE_DAYS) {
            throw new IllegalArgumentException("date range must be at most 366 days");
        }
    }

    PlanToday ensurePlanToday(UUID ownerId, String username) {
        boolean treatPlanAsWatched = store.getOrCreateProfile(ownerId, username).treatPlanAsWatched();
        return ensurePlanToday(ownerId, username, treatPlanAsWatched);
    }

    private PlanToday ensurePlanToday(UUID ownerId, String username, boolean treatPlanAsWatched) {
        LocalDate currentDate = today();
        store.getOrCreateProfile(ownerId, username);
        Optional<PlanToday> existing = store.findPlanTodayByOwner(ownerId);
        if (existing.isPresent()) {
            PlanToday plan = existing.get();
            if (plan.forDate().isAfter(currentDate)) {
                throw new PlanDateConflictException("PlanToday forDate is after today");
            }
            if (plan.forDate().equals(currentDate)) {
                return plan;
            }
            return rollPlanToday(ownerId, plan, currentDate, treatPlanAsWatched);
        }
        if (treatPlanAsWatched) {
            recordMissedForwardAsWatched(ownerId, currentDate);
        } else {
            expireForwardBefore(ownerId, currentDate);
        }
        return openPlanTodayFor(ownerId, currentDate, treatPlanAsWatched);
    }

    private void preflightEnsure(UUID ownerId) {
        LocalDate currentDate = today();
        Optional<PlanToday> existing = store.findPlanTodayByOwner(ownerId);
        if (existing.isPresent() && existing.get().forDate().isAfter(currentDate)) {
            throw new PlanDateConflictException("PlanToday forDate is after today");
        }
    }

    private PlanToday rollPlanToday(
            UUID ownerId,
            PlanToday stale,
            LocalDate currentDate,
            boolean treatPlanAsWatched
    ) {
        int flushedCount = 0;
        for (PlanTodayLine line : stale.lines()) {
            if (treatPlanAsWatched || line.checked()) {
                WatchEvent event = new WatchEvent(
                        UUID.randomUUID(),
                        ownerId,
                        stale.forDate(),
                        line.contentTitle()
                );
                store.appendWatchEvent(event);
                integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventRecorded(event));
                flushedCount++;
            }
        }
        integrationEventPublisher.publish(
                new PlannerIntegrationEvent.PlanTodayRolled(ownerId, stale.forDate(), flushedCount)
        );
        if (treatPlanAsWatched) {
            recordMissedForwardAsWatched(ownerId, currentDate);
        } else {
            expireForwardBefore(ownerId, currentDate);
        }
        return openPlanTodayFor(ownerId, currentDate, treatPlanAsWatched);
    }

    private PlanToday openPlanTodayFor(UUID ownerId, LocalDate currentDate, boolean treatPlanAsWatched) {
        PlanToday plan = PlanToday.empty(ownerId, currentDate);
        List<ForwardPlanItem> moved = store.deleteForwardPlanItemsByOwnerAndPlannedFor(ownerId, currentDate);
        for (ForwardPlanItem item : moved) {
            plan = plan.append(new PlanTodayLine(
                    UUID.randomUUID(),
                    item.contentTitle(),
                    treatPlanAsWatched,
                    PlanLineSource.FORWARD
            ));
            integrationEventPublisher.publish(new PlannerIntegrationEvent.ForwardPlanItemRemoved(
                    ownerId,
                    item.id(),
                    item.plannedFor(),
                    item.contentTitle(),
                    PlannerIntegrationEvent.ForwardPlanItemRemovalReason.MOVED_TO_TODAY
            ));
        }
        store.savePlanToday(plan);
        return plan;
    }

    private void recordMissedForwardAsWatched(UUID ownerId, LocalDate currentDate) {
        List<ForwardPlanItem> missed = store.findForwardPlanItemsByOwnerAndPlannedForBefore(ownerId, currentDate);
        for (ForwardPlanItem item : missed) {
            WatchEvent event = new WatchEvent(
                    UUID.randomUUID(),
                    ownerId,
                    item.plannedFor(),
                    item.contentTitle()
            );
            store.appendWatchEvent(event);
            integrationEventPublisher.publish(new PlannerIntegrationEvent.WatchEventRecorded(event));
        }
        store.deleteForwardPlanItemsByOwnerAndPlannedForBefore(ownerId, currentDate);
        for (ForwardPlanItem item : missed) {
            integrationEventPublisher.publish(new PlannerIntegrationEvent.ForwardPlanItemRemoved(
                    ownerId,
                    item.id(),
                    item.plannedFor(),
                    item.contentTitle(),
                    PlannerIntegrationEvent.ForwardPlanItemRemovalReason.RECORDED_AS_WATCHED
            ));
        }
    }

    private void expireForwardBefore(UUID ownerId, LocalDate currentDate) {
        List<ForwardPlanItem> expired = store.deleteForwardPlanItemsByOwnerAndPlannedForBefore(ownerId, currentDate);
        for (ForwardPlanItem item : expired) {
            integrationEventPublisher.publish(new PlannerIntegrationEvent.ForwardPlanItemRemoved(
                    ownerId,
                    item.id(),
                    item.plannedFor(),
                    item.contentTitle(),
                    PlannerIntegrationEvent.ForwardPlanItemRemovalReason.EXPIRED
            ));
        }
    }

    private <T> T inOwnerWrite(UUID ownerId, Supplier<T> action) {
        Objects.requireNonNull(ownerId, "ownerId");
        return inWriteTransaction(() -> store.withOwnerLock(ownerId, action));
    }

    private <T> T inWriteTransaction(Supplier<T> action) {
        PlatformTransactionManager transactionManager = transactionManagers.getIfAvailable();
        if (transactionManager == null) {
            return action.get();
        }
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private static void requireLine(PlanToday plan, UUID lineId) {
        boolean exists = plan.lines().stream().anyMatch(line -> line.id().equals(lineId));
        if (!exists) {
            throw new PlanResourceNotFoundException("PlanToday line not found");
        }
    }

    private PlanToday checkAllPlanTodayLines(PlanToday plan) {
        PlanToday updated = plan;
        for (PlanTodayLine line : plan.lines()) {
            if (!line.checked()) {
                updated = updated.withLineChecked(line.id(), true);
            }
        }
        if (!updated.equals(plan)) {
            store.savePlanToday(updated);
        }
        return updated;
    }

    private int projectedEnsureArchiveWrites(
            UUID ownerId,
            LocalDate currentDate,
            LocalDate watchedOn,
            boolean treatPlanAsWatched
    ) {
        int extra = 0;
        Optional<PlanToday> plan = store.findPlanTodayByOwner(ownerId);
        if (plan.isPresent()
                && plan.get().forDate().isBefore(currentDate)
                && plan.get().forDate().equals(watchedOn)) {
            extra += treatPlanAsWatched
                    ? plan.get().lines().size()
                    : (int) plan.get().lines().stream().filter(PlanTodayLine::checked).count();
        }
        if (treatPlanAsWatched) {
            extra += (int) store.findForwardPlanItemsByOwnerAndPlannedForBefore(ownerId, currentDate).stream()
                    .filter(item -> item.plannedFor().equals(watchedOn))
                    .count();
        }
        return extra;
    }

    private static void requireContentTitle(String contentTitle) {
        if (contentTitle == null || contentTitle.isBlank()) {
            throw new IllegalArgumentException("contentTitle must not be blank");
        }
    }
}
