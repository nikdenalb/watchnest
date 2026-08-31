package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.ForwardPlanItem;
import dev.watchnest.planner.domain.LibraryLimits;
import dev.watchnest.planner.domain.LibraryProfile;
import dev.watchnest.planner.domain.PlanLineSource;
import dev.watchnest.planner.domain.PlanToday;
import dev.watchnest.planner.domain.PlanTodayLine;
import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
import dev.watchnest.plannerapp.api.dto.ForwardPlanResponse;
import dev.watchnest.plannerapp.api.dto.PlanTodayLineResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventArchiveResponse;
import dev.watchnest.plannerapp.api.dto.WatchEventResponse;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 7, 6);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 7, 7);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 7, 8);
    private static final LocalDate THURSDAY = LocalDate.of(2026, 7, 9);
    private static final LocalDate PAST = LocalDate.of(2026, 7, 1);
    private static final LocalDate FUTURE = LocalDate.of(2026, 7, 10);
    private static final LocalDate FRIDAY = FUTURE;
    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private InMemoryPersonalLibraryStore store;
    private PersonalLibraryService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryPersonalLibraryStore();
        service = serviceOn(MONDAY);
    }

    @Test
    void dashboardLazyCreatesProfileAndEmptyPlanToday() {
        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");

        assertEquals("alice", dashboard.displayName());
        assertEquals(MONDAY, dashboard.today());
        assertEquals(2, dashboard.status().episodeLimit());
        assertEquals(0, dashboard.status().episodesPlanned());
        assertEquals(2, dashboard.status().episodesRemaining());
        assertTrue(dashboard.status().canAddAnotherEpisode());
        assertEquals(MONDAY, dashboard.planToday().date());
        assertTrue(dashboard.planToday().lines().isEmpty());
        assertFalse(dashboard.treatPlanAsWatched());
        assertEquals(2, dashboard.policy().weekdayEpisodeLimit());
        assertEquals(4, dashboard.policy().weekendEpisodeLimit());
    }

    @Test
    void addPlanTodayLineStoresTrimmedTitleWithoutWatchEvent() {
        PlanTodayLineResponse response = service.addPlanTodayLine(ALICE_ID, "alice", "  Blue Tractor  ");

        assertEquals("Blue Tractor", response.contentTitle());
        assertEquals(PlanLineSource.MANUAL, response.source());
        assertFalse(response.checked());

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, dashboard.status().episodesPlanned());
        assertEquals(1, dashboard.planToday().lines().size());
        assertEquals("Blue Tractor", dashboard.planToday().lines().getFirst().contentTitle());

        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, "alice", MONDAY, MONDAY);
        assertTrue(archive.events().isEmpty());
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
    }

    @Test
    void addPlanTodayLineRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> service.addPlanTodayLine(ALICE_ID, "alice", "   "));
        assertThrows(IllegalArgumentException.class, () -> service.addPlanTodayLine(ALICE_ID, "alice", null));
        assertTrue(store.findPlanTodayByOwner(ALICE_ID).isEmpty());
    }

    @Test
    void updatePolicyChangesLimitsAndPublishesEvent() {
        service.updateScreenTimePolicy(ALICE_ID, "alice", 3, 5);

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(3, dashboard.policy().weekdayEpisodeLimit());
        assertEquals(5, dashboard.policy().weekendEpisodeLimit());
        assertEquals(3, dashboard.status().episodeLimit());

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        assertTrue(captor.getValue() instanceof PlannerIntegrationEvent.ScreenTimePolicyUpdated);
    }

    @Test
    void planTodayPreservesInsertionOrder() {
        service.addPlanTodayLine(ALICE_ID, "alice", "Zebra");
        service.addPlanTodayLine(ALICE_ID, "alice", "Alpha");

        List<String> titles = service.dashboard(ALICE_ID, "alice").planToday().lines().stream()
                .map(PlanTodayLineResponse::contentTitle)
                .toList();
        assertEquals(List.of("Zebra", "Alpha"), titles);
    }

    @Test
    void ownersAreIsolated() {
        service.addPlanTodayLine(ALICE_ID, "alice", "Alice Show");
        service.updateScreenTimePolicy(ALICE_ID, "alice", 7, 8);

        DashboardResponse bob = service.dashboard(BOB_ID, "bob");
        assertEquals("bob", bob.displayName());
        assertTrue(bob.planToday().lines().isEmpty());
        assertEquals(2, bob.policy().weekdayEpisodeLimit());
        assertEquals(4, bob.policy().weekendEpisodeLimit());
        assertEquals(0, bob.status().episodesPlanned());

        DashboardResponse alice = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, alice.planToday().lines().size());
        assertEquals(7, alice.policy().weekdayEpisodeLimit());
        assertNotEquals(bob.policy().weekdayEpisodeLimit(), alice.policy().weekdayEpisodeLimit());
    }

    @Test
    void todayMovePutsOnlyTodayItemsOnPlanToday() {
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, MONDAY, "Today Move"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, FUTURE, "Future Stay"));

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, dashboard.planToday().lines().size());
        assertEquals("Today Move", dashboard.planToday().lines().getFirst().contentTitle());
        assertEquals(PlanLineSource.FORWARD, dashboard.planToday().lines().getFirst().source());

        ForwardPlanResponse forward = service.forwardPlan(ALICE_ID, "alice", MONDAY, FUTURE);
        assertEquals(1, forward.items().size());
        assertEquals("Future Stay", forward.items().getFirst().contentTitle());
        assertEquals(FUTURE, forward.items().getFirst().plannedFor());
    }

    @Test
    void expiredForwardItemsAreDeletedWithoutInventingPlanOrWatchEvents() {
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, PAST, "Missed"));

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertTrue(dashboard.planToday().lines().isEmpty());
        assertEquals(MONDAY, dashboard.planToday().date());

        ForwardPlanResponse forward = service.forwardPlan(ALICE_ID, "alice", PAST, FUTURE);
        assertTrue(forward.items().isEmpty());
        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, "alice", PAST, PAST);
        assertTrue(archive.events().isEmpty());
    }

    @Test
    void quotaUsesLineCountAndIgnoreCheckedToggle() {
        service.addPlanTodayLine(ALICE_ID, "alice", "One");
        DashboardResponse afterAdd = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, afterAdd.status().episodesPlanned());
        assertEquals(1, afterAdd.status().episodesRemaining());
        assertTrue(afterAdd.status().canAddAnotherEpisode());

        UUID lineId = afterAdd.planToday().lines().getFirst().id();
        service.patchPlanTodayLine(ALICE_ID, "alice", lineId, true);

        DashboardResponse afterCheck = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, afterCheck.status().episodesPlanned());
        assertEquals(1, afterCheck.status().episodesRemaining());
        assertTrue(afterCheck.planToday().lines().getFirst().checked());
        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, "alice", MONDAY, MONDAY);
        assertTrue(archive.events().isEmpty());
    }

    @Test
    void rollFlushesCheckedToClosedDateAndMovesNewDayItems() {
        PlanTodayLineResponse checked = service.addPlanTodayLine(ALICE_ID, "alice", "Watched");
        PlanTodayLineResponse leftover = service.addPlanTodayLine(ALICE_ID, "alice", "Leftover");
        service.patchPlanTodayLine(ALICE_ID, "alice", checked.id(), true);
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, THURSDAY, "Thursday Move"));

        PersonalLibraryService thursday = serviceOn(THURSDAY);
        DashboardResponse dashboard = thursday.dashboard(ALICE_ID, "alice");

        assertEquals(THURSDAY, dashboard.planToday().date());
        assertEquals(1, dashboard.planToday().lines().size());
        assertEquals("Thursday Move", dashboard.planToday().lines().getFirst().contentTitle());
        assertEquals(PlanLineSource.FORWARD, dashboard.planToday().lines().getFirst().source());

        WatchEventArchiveResponse mondayArchive = thursday.watchEventArchive(ALICE_ID, "alice", MONDAY, MONDAY);
        assertEquals(1, mondayArchive.events().size());
        assertEquals("Watched", mondayArchive.events().getFirst().contentTitle());
        assertEquals(MONDAY, mondayArchive.events().getFirst().watchedOn());
        assertTrue(mondayArchive.events().stream().noneMatch(event -> event.contentTitle().equals("Leftover")));
        assertNotEquals(leftover.id(), dashboard.planToday().lines().getFirst().id());
    }

    @Test
    void skipGapDaysExpiresTueWedAndOpensThursdayOnly() {
        service.addPlanTodayLine(ALICE_ID, "alice", "Monday leftover");
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, TUESDAY, "Tue"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, WEDNESDAY, "Wed"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, THURSDAY, "Thu"));

        PersonalLibraryService thursday = serviceOn(THURSDAY);
        DashboardResponse dashboard = thursday.dashboard(ALICE_ID, "alice");

        assertEquals(THURSDAY, dashboard.planToday().date());
        assertEquals(List.of("Thu"), dashboard.planToday().lines().stream()
                .map(PlanTodayLineResponse::contentTitle)
                .toList());
        ForwardPlanResponse forward = thursday.forwardPlan(ALICE_ID, "alice", TUESDAY, THURSDAY);
        assertTrue(forward.items().isEmpty());
        assertTrue(thursday.watchEventArchive(ALICE_ID, "alice", TUESDAY, WEDNESDAY).events().isEmpty());
        assertEquals(THURSDAY, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
    }

    @Test
    void futurePlanTodayConflictsWithoutFlushOrCleanup() {
        store.savePlanToday(new PlanToday(
                ALICE_ID,
                FUTURE,
                List.of(new PlanTodayLine(UUID.randomUUID(), "Future line", true, PlanLineSource.MANUAL))
        ));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, PAST, "Expired-looking"));

        assertThrows(PlanDateConflictException.class, () -> service.dashboard(ALICE_ID, "alice"));
        assertEquals(FUTURE, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
        assertEquals(1, store.countForwardPlanItemsByOwnerAndPlannedFor(ALICE_ID, PAST));
        assertTrue(store.findWatchEventsByOwnerAndWatchedOnBetween(ALICE_ID, PAST, FUTURE).isEmpty());
    }

    @Test
    void sequentialEnsureOnSameDayIsIdempotent() {
        DashboardResponse first = service.dashboard(ALICE_ID, "alice");
        PlanTodayLineResponse line = service.addPlanTodayLine(ALICE_ID, "alice", "Keep");
        DashboardResponse second = service.dashboard(ALICE_ID, "alice");

        assertEquals(first.planToday().date(), second.planToday().date());
        assertEquals(line.id(), second.planToday().lines().getFirst().id());
        assertEquals(1, second.planToday().lines().size());
    }

    @Test
    void patchAndDeleteAfterRollReturnNotFound() {
        PlanTodayLineResponse line = service.addPlanTodayLine(ALICE_ID, "alice", "Old");
        PersonalLibraryService thursday = serviceOn(THURSDAY);
        thursday.dashboard(ALICE_ID, "alice");

        assertThrows(
                PlanResourceNotFoundException.class,
                () -> thursday.patchPlanTodayLine(ALICE_ID, "alice", line.id(), true)
        );
        assertThrows(
                PlanResourceNotFoundException.class,
                () -> thursday.deletePlanTodayLine(ALICE_ID, "alice", line.id())
        );
    }

    @Test
    void forwardPostRejectsTodayAndPastAndGetReturnsInsertionOrder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addForwardPlanItem(ALICE_ID, "alice", MONDAY, "Today")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addForwardPlanItem(ALICE_ID, "alice", PAST, "Past")
        );
        assertTrue(store.findForwardPlanItemsByOwnerAndPlannedForBetween(ALICE_ID, PAST, FUTURE).isEmpty());

        service.addForwardPlanItem(ALICE_ID, "alice", FUTURE.plusDays(1), "Later Z");
        service.addForwardPlanItem(ALICE_ID, "alice", FUTURE, "B first");
        service.addForwardPlanItem(ALICE_ID, "alice", FUTURE, "A second");

        ForwardPlanResponse forward = service.forwardPlan(ALICE_ID, "alice", FUTURE, FUTURE.plusDays(1));
        assertEquals(List.of("B first", "A second", "Later Z"), forward.items().stream()
                .map(item -> item.contentTitle())
                .toList());
    }

    @Test
    void forwardMutationsPublishAddedDeletedMovedAndExpiredReasons() {
        var added = service.addForwardPlanItem(ALICE_ID, "alice", FUTURE, "Keep");
        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.ForwardPlanItemAdded.class));

        clearInvocations(integrationEventPublisher);
        service.deleteForwardPlanItem(ALICE_ID, "alice", added.id());
        ArgumentCaptor<PlannerIntegrationEvent.ForwardPlanItemRemoved> removed =
                ArgumentCaptor.forClass(PlannerIntegrationEvent.ForwardPlanItemRemoved.class);
        verify(integrationEventPublisher).publish(removed.capture());
        assertEquals(PlannerIntegrationEvent.ForwardPlanItemRemovalReason.USER_DELETED, removed.getValue().reason());

        clearInvocations(integrationEventPublisher);
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), BOB_ID, PAST, "Missed"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), BOB_ID, MONDAY, "Move me"));
        service.dashboard(BOB_ID, "bob");

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher, org.mockito.Mockito.atLeast(2)).publish(captor.capture());
        List<PlannerIntegrationEvent.ForwardPlanItemRemovalReason> reasons = captor.getAllValues().stream()
                .filter(PlannerIntegrationEvent.ForwardPlanItemRemoved.class::isInstance)
                .map(PlannerIntegrationEvent.ForwardPlanItemRemoved.class::cast)
                .map(PlannerIntegrationEvent.ForwardPlanItemRemoved::reason)
                .toList();
        assertTrue(reasons.contains(PlannerIntegrationEvent.ForwardPlanItemRemovalReason.EXPIRED));
        assertTrue(reasons.contains(PlannerIntegrationEvent.ForwardPlanItemRemovalReason.MOVED_TO_TODAY));
    }

    @Test
    void archiveFiltersByInclusiveRangeAndOwner() {
        seedWatch(ALICE_ID, PAST, "Past Show");
        seedWatch(ALICE_ID, MONDAY, "Today Show");
        seedWatch(ALICE_ID, FUTURE, "Future Show");
        seedWatch(BOB_ID, MONDAY, "Bob Show");

        WatchEventArchiveResponse onlyPast = service.watchEventArchive(ALICE_ID, "alice", PAST, PAST);
        assertEquals(PAST, onlyPast.from());
        assertEquals(PAST, onlyPast.to());
        assertEquals(1, onlyPast.events().size());
        assertEquals("Past Show", onlyPast.events().getFirst().contentTitle());

        WatchEventArchiveResponse throughToday = service.watchEventArchive(ALICE_ID, "alice", PAST, MONDAY);
        assertEquals(2, throughToday.events().size());
        assertEquals("Today Show", throughToday.events().get(0).contentTitle());
        assertEquals("Past Show", throughToday.events().get(1).contentTitle());

        WatchEventArchiveResponse bob = service.watchEventArchive(BOB_ID, "bob", PAST, FUTURE);
        assertEquals(1, bob.events().size());
        assertEquals("Bob Show", bob.events().getFirst().contentTitle());
        assertEquals(BOB_ID, bob.events().getFirst().ownerId());
    }

    @Test
    void archiveOrdersByWatchedOnDescThenTitleAsc() {
        seedWatch(ALICE_ID, FUTURE, "Zebra");
        seedWatch(ALICE_ID, FUTURE, "Alpha");
        seedWatch(ALICE_ID, MONDAY, "Mid");
        seedWatch(ALICE_ID, PAST, "Early");

        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, "alice", PAST, FUTURE);
        assertEquals(4, archive.events().size());
        assertEquals(FUTURE, archive.events().get(0).watchedOn());
        assertEquals("Alpha", archive.events().get(0).contentTitle());
        assertEquals(FUTURE, archive.events().get(1).watchedOn());
        assertEquals("Zebra", archive.events().get(1).contentTitle());
        assertEquals(MONDAY, archive.events().get(2).watchedOn());
        assertEquals("Mid", archive.events().get(2).contentTitle());
        assertEquals(PAST, archive.events().get(3).watchedOn());
        assertEquals("Early", archive.events().get(3).contentTitle());
    }

    @Test
    void archiveRejectsInvertedAndOversizedRangeBeforeEnsure() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.watchEventArchive(ALICE_ID, "alice", FUTURE, PAST)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.watchEventArchive(ALICE_ID, "alice", MONDAY.minusDays(366), MONDAY)
        );
        assertTrue(store.findPlanTodayByOwner(ALICE_ID).isEmpty());
        WatchEventArchiveResponse maxRange = service.watchEventArchive(
                ALICE_ID,
                "alice",
                MONDAY.minusDays(365),
                MONDAY
        );
        assertTrue(maxRange.events().isEmpty());
    }

    @Test
    void archiveDoesNotPublishWhenPlanTodayAlreadyOpen() {
        seedWatch(ALICE_ID, PAST, "Past Show");
        service.addPlanTodayLine(ALICE_ID, "alice", "Today Show");
        clearInvocations(integrationEventPublisher);

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, dashboard.status().episodesPlanned());
        assertEquals("Today Show", dashboard.planToday().lines().getFirst().contentTitle());

        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, "alice", PAST, FUTURE);
        assertEquals(1, archive.events().size());
        verify(integrationEventPublisher, never()).publish(any());
    }

    @Test
    void rollPublishesWatchEventsThenPlanTodayRolled() {
        PlanTodayLineResponse checked = service.addPlanTodayLine(ALICE_ID, "alice", "Watched");
        service.patchPlanTodayLine(ALICE_ID, "alice", checked.id(), true);
        clearInvocations(integrationEventPublisher);

        serviceOn(THURSDAY).dashboard(ALICE_ID, "alice");

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher, org.mockito.Mockito.atLeast(2)).publish(captor.capture());
        assertTrue(captor.getAllValues().getFirst() instanceof PlannerIntegrationEvent.WatchEventRecorded);
        assertTrue(captor.getAllValues().stream().anyMatch(PlannerIntegrationEvent.PlanTodayRolled.class::isInstance));
        PlannerIntegrationEvent.PlanTodayRolled rolled = captor.getAllValues().stream()
                .filter(PlannerIntegrationEvent.PlanTodayRolled.class::isInstance)
                .map(PlannerIntegrationEvent.PlanTodayRolled.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(MONDAY, rolled.closedDate());
        assertEquals(1, rolled.flushedCount());
    }

    @Test
    void addPastWatchEventAppearsInArchiveWithoutChangingTodayQuota() {
        service.dashboard(ALICE_ID, "alice");
        LocalDate yesterday = MONDAY.minusDays(1);

        WatchEventResponse added = service.addWatchEvent(ALICE_ID, "alice", yesterday, "Yesterday Show");

        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, "alice", yesterday, yesterday);
        assertEquals(1, archive.events().size());
        assertEquals(added.id(), archive.events().getFirst().id());
        assertEquals("Yesterday Show", archive.events().getFirst().contentTitle());
        assertEquals(yesterday, archive.events().getFirst().watchedOn());
        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(0, dashboard.status().episodesPlanned());
        assertEquals(MONDAY, dashboard.planToday().date());
        assertTrue(dashboard.planToday().lines().isEmpty());
    }

    @Test
    void addTodayWatchEventIsRejectedWithoutFlushingStalePlanToday() {
        LocalDate yesterday = MONDAY.minusDays(1);
        seedStaleCheckedPlanToday(ALICE_ID, yesterday, "Stale watched");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", MONDAY, "Today Show")
        );

        assertTrue(store.findWatchEventsByOwnerAndWatchedOnBetween(ALICE_ID, MONDAY, MONDAY).isEmpty());
        assertEquals(yesterday, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.PlanTodayRolled.class));
    }

    @Test
    void addFutureWatchEventIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", MONDAY.plusDays(1), "Tomorrow Show")
        );
        assertTrue(store.findWatchEventsByOwnerAndWatchedOnBetween(ALICE_ID, PAST, FUTURE).isEmpty());
        verify(integrationEventPublisher, never()).publish(any());
    }

    @Test
    void renamePastWatchEventChangesTitleKeepsWatchedOnAndPublishesCorrected() {
        WatchEventResponse added = service.addWatchEvent(ALICE_ID, "alice", MONDAY.minusDays(1), "Old Title");
        clearInvocations(integrationEventPublisher);

        WatchEventResponse patched = service.patchWatchEvent(ALICE_ID, "alice", added.id(), "New Title");

        assertEquals("New Title", patched.contentTitle());
        assertEquals(added.watchedOn(), patched.watchedOn());
        assertEquals(added.id(), patched.id());
        ArgumentCaptor<PlannerIntegrationEvent.WatchEventCorrected> captor =
                ArgumentCaptor.forClass(PlannerIntegrationEvent.WatchEventCorrected.class);
        verify(integrationEventPublisher).publish(captor.capture());
        assertEquals("Old Title", captor.getValue().previousTitle());
        assertEquals("New Title", captor.getValue().updated().contentTitle());
        assertEquals(added.watchedOn(), captor.getValue().updated().watchedOn());
    }

    @Test
    void patchSameTrimmedTitleStillEnsuresAndDoesNotPublishCorrected() {
        LocalDate yesterday = MONDAY.minusDays(1);
        WatchEvent seeded = seedWatch(ALICE_ID, PAST, "Keep");
        seedStaleCheckedPlanToday(ALICE_ID, yesterday, "Stale watched");
        clearInvocations(integrationEventPublisher);

        WatchEventResponse patched = service.patchWatchEvent(ALICE_ID, "alice", seeded.id(), "  Keep  ");

        assertEquals("Keep", patched.contentTitle());
        assertEquals(PAST, patched.watchedOn());
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventCorrected.class));
        assertEquals(MONDAY, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
        WatchEventArchiveResponse flushed = service.watchEventArchive(ALICE_ID, "alice", yesterday, yesterday);
        assertEquals(1, flushed.events().size());
        assertEquals("Stale watched", flushed.events().getFirst().contentTitle());
    }

    @Test
    void deletePastWatchEventPublishesDeletedAndSecondDeleteIsNotFound() {
        LocalDate yesterday = MONDAY.minusDays(1);
        WatchEventResponse added = service.addWatchEvent(ALICE_ID, "alice", yesterday, "Gone");
        clearInvocations(integrationEventPublisher);
        service.deleteWatchEvent(ALICE_ID, "alice", added.id());

        assertTrue(service.watchEventArchive(ALICE_ID, "alice", yesterday, yesterday).events().isEmpty());
        ArgumentCaptor<PlannerIntegrationEvent.WatchEventDeleted> deleted =
                ArgumentCaptor.forClass(PlannerIntegrationEvent.WatchEventDeleted.class);
        verify(integrationEventPublisher).publish(deleted.capture());
        assertEquals(added.id(), deleted.getValue().id());
        assertEquals("Gone", deleted.getValue().contentTitle());
        assertEquals(yesterday, deleted.getValue().watchedOn());

        seedStaleCheckedPlanToday(ALICE_ID, yesterday, "Stale watched");
        clearInvocations(integrationEventPublisher);
        assertThrows(
                PlanResourceNotFoundException.class,
                () -> service.deleteWatchEvent(ALICE_ID, "alice", added.id())
        );
        verify(integrationEventPublisher, never()).publish(any());
        assertEquals(yesterday, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
    }

    @Test
    void otherOwnerWatchEventIdIsNotFoundWithoutEnsure() {
        LocalDate yesterday = MONDAY.minusDays(1);
        WatchEvent bobs = seedWatch(BOB_ID, yesterday, "Bob Show");
        seedStaleCheckedPlanToday(ALICE_ID, yesterday, "Stale watched");
        clearInvocations(integrationEventPublisher);

        assertThrows(
                PlanResourceNotFoundException.class,
                () -> service.patchWatchEvent(ALICE_ID, "alice", bobs.id(), "Hijack")
        );
        assertThrows(
                PlanResourceNotFoundException.class,
                () -> service.deleteWatchEvent(ALICE_ID, "alice", bobs.id())
        );
        verify(integrationEventPublisher, never()).publish(any());
        assertEquals(yesterday, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
        assertEquals("Bob Show", store.findWatchEventByOwnerAndId(BOB_ID, bobs.id()).orElseThrow().contentTitle());
    }

    @Test
    void fiftyWatchEventsOnOnePastDateAreAcceptedAndFiftyFirstIsRejected() {
        LocalDate yesterday = MONDAY.minusDays(1);
        for (int index = 1; index <= LibraryLimits.MAX_TITLES_PER_DATE; index++) {
            service.addWatchEvent(ALICE_ID, "alice", yesterday, "Show " + index);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", yesterday, "Overflow")
        );
        assertEquals(LibraryLimits.MAX_TITLES_PER_DATE, store.countWatchEventsByOwnerAndWatchedOn(ALICE_ID, yesterday));
    }

    @Test
    void archiveCapCountsStaleCheckedPlanTodayLinesAndDoesNotRoll() {
        LocalDate yesterday = MONDAY.minusDays(1);
        for (int index = 0; index < LibraryLimits.MAX_TITLES_PER_DATE - 1; index++) {
            seedWatch(ALICE_ID, yesterday, "Archived " + index);
        }
        seedStaleCheckedPlanToday(ALICE_ID, yesterday, "Stale checked");
        clearInvocations(integrationEventPublisher);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", yesterday, "One more")
        );

        assertEquals(
                LibraryLimits.MAX_TITLES_PER_DATE - 1,
                store.countWatchEventsByOwnerAndWatchedOn(ALICE_ID, yesterday)
        );
        assertEquals(yesterday, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
        verify(integrationEventPublisher, never()).publish(any());
    }

    @Test
    void addWatchEventRequiresWatchedOn() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", null, "Hello")
        );
        assertTrue(store.findWatchEventsByOwnerAndWatchedOnBetween(ALICE_ID, PAST, FUTURE).isEmpty());
    }

    @Test
    void addWatchEventStoresTrimmedTitle() {
        WatchEventResponse added = service.addWatchEvent(ALICE_ID, "alice", MONDAY.minusDays(1), "  Hello  ");
        assertEquals("Hello", added.contentTitle());
        assertEquals(
                "Hello",
                store.findWatchEventByOwnerAndId(ALICE_ID, added.id()).orElseThrow().contentTitle()
        );
    }

    @Test
    void treatPlanAsWatchedChecksAllLinesAndRejectsCheckedPatch() {
        service.addPlanTodayLine(ALICE_ID, "alice", "One");
        service.addPlanTodayLine(ALICE_ID, "alice", "Two");
        assertFalse(service.dashboard(ALICE_ID, "alice").planToday().lines().getFirst().checked());

        service.updateLibraryPreferences(ALICE_ID, "alice", true);

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertTrue(dashboard.treatPlanAsWatched());
        assertTrue(dashboard.planToday().lines().stream().allMatch(PlanTodayLineResponse::checked));

        PlanTodayLineResponse added = service.addPlanTodayLine(ALICE_ID, "alice", "Three");
        assertTrue(added.checked());

        UUID lineId = dashboard.planToday().lines().getFirst().id();
        assertThrows(
                IllegalArgumentException.class,
                () -> service.patchPlanTodayLine(ALICE_ID, "alice", lineId, false)
        );
        service.deletePlanTodayLine(ALICE_ID, "alice", added.id());
        assertEquals(2, service.dashboard(ALICE_ID, "alice").planToday().lines().size());
    }

    @Test
    void treatPlanAsWatchedSkipDaysArchivesAllPlanTodayAndMissedForward() {
        service.addPlanTodayLine(ALICE_ID, "alice", "Mon leftover");
        PlanTodayLineResponse watched = service.addPlanTodayLine(ALICE_ID, "alice", "Mon watched");
        service.patchPlanTodayLine(ALICE_ID, "alice", watched.id(), true);
        service.updateLibraryPreferences(ALICE_ID, "alice", true);
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, TUESDAY, "Tue"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, WEDNESDAY, "Wed"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, THURSDAY, "Thu"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, FRIDAY, "Friday Move"));

        PersonalLibraryService friday = serviceOn(FRIDAY);
        DashboardResponse dashboard = friday.dashboard(ALICE_ID, "alice");

        assertEquals(FRIDAY, dashboard.planToday().date());
        assertEquals(1, dashboard.planToday().lines().size());
        assertEquals("Friday Move", dashboard.planToday().lines().getFirst().contentTitle());
        assertTrue(dashboard.planToday().lines().getFirst().checked());
        assertEquals(PlanLineSource.FORWARD, dashboard.planToday().lines().getFirst().source());

        WatchEventArchiveResponse monday = friday.watchEventArchive(ALICE_ID, "alice", MONDAY, MONDAY);
        assertEquals(2, monday.events().size());
        assertTrue(monday.events().stream().anyMatch(event -> event.contentTitle().equals("Mon leftover")));
        assertTrue(monday.events().stream().anyMatch(event -> event.contentTitle().equals("Mon watched")));
        assertEquals("Tue", friday.watchEventArchive(ALICE_ID, "alice", TUESDAY, TUESDAY).events().getFirst().contentTitle());
        assertEquals("Wed", friday.watchEventArchive(ALICE_ID, "alice", WEDNESDAY, WEDNESDAY).events().getFirst().contentTitle());
        assertEquals("Thu", friday.watchEventArchive(ALICE_ID, "alice", THURSDAY, THURSDAY).events().getFirst().contentTitle());
        assertTrue(friday.forwardPlan(ALICE_ID, "alice", TUESDAY, FRIDAY).items().isEmpty());
    }

    @Test
    void flagOffSkipDaysExpiresMissedForwardAndMovesFridayUnchecked() {
        service.addPlanTodayLine(ALICE_ID, "alice", "Mon leftover");
        PlanTodayLineResponse watched = service.addPlanTodayLine(ALICE_ID, "alice", "Mon watched");
        service.patchPlanTodayLine(ALICE_ID, "alice", watched.id(), true);
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, TUESDAY, "Tue"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, WEDNESDAY, "Wed"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, THURSDAY, "Thu"));
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, FRIDAY, "Friday Move"));

        PersonalLibraryService friday = serviceOn(FRIDAY);
        DashboardResponse dashboard = friday.dashboard(ALICE_ID, "alice");

        assertEquals("Friday Move", dashboard.planToday().lines().getFirst().contentTitle());
        assertFalse(dashboard.planToday().lines().getFirst().checked());
        assertEquals(1, friday.watchEventArchive(ALICE_ID, "alice", MONDAY, MONDAY).events().size());
        assertEquals("Mon watched", friday.watchEventArchive(ALICE_ID, "alice", MONDAY, MONDAY).events().getFirst().contentTitle());
        assertTrue(friday.watchEventArchive(ALICE_ID, "alice", TUESDAY, THURSDAY).events().isEmpty());
        assertTrue(friday.forwardPlan(ALICE_ID, "alice", TUESDAY, THURSDAY).items().isEmpty());
    }

    @Test
    void turningFlagOnAfterFalseRollDoesNotRecreateExpiredForward() {
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), ALICE_ID, PAST, "Missed"));
        service.dashboard(ALICE_ID, "alice");
        assertTrue(service.watchEventArchive(ALICE_ID, "alice", PAST, PAST).events().isEmpty());

        service.updateLibraryPreferences(ALICE_ID, "alice", true);

        assertTrue(service.watchEventArchive(ALICE_ID, "alice", PAST, PAST).events().isEmpty());
        assertTrue(store.findForwardPlanItemsByOwnerAndPlannedForBefore(ALICE_ID, MONDAY).isEmpty());
    }

    @Test
    void turningFlagOffLeavesCheckmarksAndAllowsPatch() {
        service.addPlanTodayLine(ALICE_ID, "alice", "Keep");
        service.updateLibraryPreferences(ALICE_ID, "alice", true);
        service.updateLibraryPreferences(ALICE_ID, "alice", false);

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertFalse(dashboard.treatPlanAsWatched());
        assertTrue(dashboard.planToday().lines().getFirst().checked());
        UUID lineId = dashboard.planToday().lines().getFirst().id();
        PlanTodayLineResponse patched = service.patchPlanTodayLine(ALICE_ID, "alice", lineId, false);
        assertFalse(patched.checked());
    }

    @Test
    void sharedLibraryLimitsCapPlanTodayForwardAndArchiveAdds() {
        int cap = LibraryLimits.MAX_TITLES_PER_DATE;
        for (int index = 0; index < cap; index++) {
            service.addPlanTodayLine(ALICE_ID, "alice", "Line " + index);
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addPlanTodayLine(ALICE_ID, "alice", "Overflow")
        );

        LocalDate futureDate = FUTURE.plusDays(1);
        for (int index = 0; index < cap; index++) {
            service.addForwardPlanItem(ALICE_ID, "alice", futureDate, "Fwd " + index);
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addForwardPlanItem(ALICE_ID, "alice", futureDate, "Overflow")
        );

        LocalDate yesterday = MONDAY.minusDays(1);
        for (int index = 0; index < cap; index++) {
            seedWatch(BOB_ID, yesterday, "Archived " + index);
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(BOB_ID, "bob", yesterday, "Overflow")
        );
        assertEquals(cap, store.countWatchEventsByOwnerAndWatchedOn(BOB_ID, yesterday)        );
    }

    @Test
    void archivePostProjectsFlagOnAllLinesAndMissedForwardWithoutRolling() {
        LocalDate yesterday = MONDAY.minusDays(1);
        for (int index = 0; index < LibraryLimits.MAX_TITLES_PER_DATE - 1; index++) {
            seedWatch(ALICE_ID, yesterday, "Archived " + index);
        }
        store.savePlanToday(new PlanToday(
                ALICE_ID,
                yesterday,
                List.of(new PlanTodayLine(UUID.randomUUID(), "Unchecked stale", false, PlanLineSource.MANUAL))
        ));
        setTreatPlanAsWatched(ALICE_ID, "alice", true);
        clearInvocations(integrationEventPublisher);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", yesterday, "One more")
        );
        assertEquals(yesterday, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
        verify(integrationEventPublisher, never()).publish(any());

        for (int index = 0; index < LibraryLimits.MAX_TITLES_PER_DATE - 1; index++) {
            seedWatch(BOB_ID, yesterday, "Bob archived " + index);
        }
        store.appendForwardPlanItem(new ForwardPlanItem(UUID.randomUUID(), BOB_ID, yesterday, "Missed"));
        setTreatPlanAsWatched(BOB_ID, "bob", true);
        clearInvocations(integrationEventPublisher);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(BOB_ID, "bob", yesterday, "One more")
        );
        assertEquals(1, store.countForwardPlanItemsByOwnerAndPlannedFor(BOB_ID, yesterday));
        verify(integrationEventPublisher, never()).publish(any());
    }

    @Test
    void updatingPolicyAfterEnablingPreservesTreatPlanAsWatched() {
        service.updateLibraryPreferences(ALICE_ID, "alice", true);
        service.updateScreenTimePolicy(ALICE_ID, "alice", 3, 5);

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertTrue(dashboard.treatPlanAsWatched());
        assertEquals(3, dashboard.policy().weekdayEpisodeLimit());
        assertEquals(5, dashboard.policy().weekendEpisodeLimit());
    }

    @Test
    void preferenceNullIsRejectedAndSameValuePutDoesNotPublish() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateLibraryPreferences(ALICE_ID, "alice", null)
        );
        assertTrue(store.findPlanTodayByOwner(ALICE_ID).isEmpty());

        service.dashboard(ALICE_ID, "alice");
        clearInvocations(integrationEventPublisher);
        service.updateLibraryPreferences(ALICE_ID, "alice", false);
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.LibraryPreferencesUpdated.class));

        service.updateLibraryPreferences(ALICE_ID, "alice", true);
        seedStaleCheckedPlanToday(ALICE_ID, PAST, "Stale");
        UUID lineId = UUID.randomUUID();
        store.savePlanToday(new PlanToday(
                ALICE_ID,
                PAST,
                List.of(new PlanTodayLine(lineId, "Stale", true, PlanLineSource.MANUAL))
        ));
        clearInvocations(integrationEventPublisher);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.patchPlanTodayLine(ALICE_ID, "alice", lineId, false)
        );
        verify(integrationEventPublisher, never()).publish(any());
        assertEquals(PAST, store.findPlanTodayByOwner(ALICE_ID).orElseThrow().forDate());
    }

    private PersonalLibraryService serviceOn(LocalDate date) {
        Clock clock = Clock.fixed(date.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new PersonalLibraryService(
                clock,
                new ScreenTimeQuotaCalculator(),
                integrationEventPublisher,
                store
        );
    }

    private WatchEvent seedWatch(UUID ownerId, LocalDate watchedOn, String title) {
        WatchEvent event = new WatchEvent(UUID.randomUUID(), ownerId, watchedOn, title);
        store.appendWatchEvent(event);
        return event;
    }

    private void seedStaleCheckedPlanToday(UUID ownerId, LocalDate forDate, String title) {
        store.savePlanToday(new PlanToday(
                ownerId,
                forDate,
                List.of(new PlanTodayLine(UUID.randomUUID(), title, true, PlanLineSource.MANUAL))
        ));
    }

    private void setTreatPlanAsWatched(UUID ownerId, String username, boolean value) {
        LibraryProfile current = store.getOrCreateProfile(ownerId, username);
        store.saveProfile(new LibraryProfile(
                current.id(),
                current.displayName(),
                current.screenTimePolicy(),
                value
        ));
    }
}
