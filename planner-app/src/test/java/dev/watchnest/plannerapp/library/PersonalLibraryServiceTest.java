package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.WatchEvent;
import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceTest {

    private static final LocalDate WEEKDAY = LocalDate.of(2026, 7, 6);
    private static final LocalDate PAST = LocalDate.of(2026, 7, 1);
    private static final LocalDate FUTURE = LocalDate.of(2026, 7, 10);
    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    @Mock
    private ObjectProvider<PlatformTransactionManager> transactionManagers;

    private InMemoryPersonalLibraryStore store;
    private PersonalLibraryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
        store = new InMemoryPersonalLibraryStore();
        service = new PersonalLibraryService(
                clock,
                new ScreenTimeQuotaCalculator(),
                integrationEventPublisher,
                store,
                transactionManagers
        );
    }

    @Test
    void dashboardLazyCreatesProfileFromUsername() {
        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");

        assertEquals("alice", dashboard.displayName());
        assertEquals(WEEKDAY, dashboard.today());
        assertEquals(2, dashboard.status().episodeLimit());
        assertEquals(0, dashboard.status().episodesWatched());
        assertEquals(2, dashboard.status().episodesRemaining());
        assertTrue(dashboard.todayEvents().isEmpty());
        assertEquals(2, dashboard.policy().weekdayEpisodeLimit());
        assertEquals(4, dashboard.policy().weekendEpisodeLimit());
    }

    @Test
    void logWatchEventStoresTrimmedTitleAndPublishesEvent() {
        WatchEventResponse response = service.logWatchEvent(ALICE_ID, "alice", "  Blue Tractor  ");

        assertEquals("Blue Tractor", response.contentTitle());
        assertEquals(WEEKDAY, response.watchedOn());
        assertEquals(ALICE_ID, response.ownerId());

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, dashboard.status().episodesWatched());
        assertEquals(1, dashboard.todayEvents().size());
        assertEquals("Blue Tractor", dashboard.todayEvents().getFirst().contentTitle());

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        assertTrue(captor.getValue() instanceof PlannerIntegrationEvent.WatchEventRecorded);
    }

    @Test
    void logWatchEventRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> service.logWatchEvent(ALICE_ID, "alice", "   "));
        assertThrows(IllegalArgumentException.class, () -> service.logWatchEvent(ALICE_ID, "alice", null));
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
    void todayEventsAreSortedByTitle() {
        service.logWatchEvent(ALICE_ID, "alice", "Zebra");
        service.logWatchEvent(ALICE_ID, "alice", "Alpha");

        assertEquals("Alpha", service.dashboard(ALICE_ID, "alice").todayEvents().get(0).contentTitle());
        assertEquals("Zebra", service.dashboard(ALICE_ID, "alice").todayEvents().get(1).contentTitle());
    }

    @Test
    void ownersAreIsolated() {
        service.logWatchEvent(ALICE_ID, "alice", "Alice Show");
        service.updateScreenTimePolicy(ALICE_ID, "alice", 7, 8);

        DashboardResponse bob = service.dashboard(BOB_ID, "bob");
        assertEquals("bob", bob.displayName());
        assertTrue(bob.todayEvents().isEmpty());
        assertEquals(2, bob.policy().weekdayEpisodeLimit());
        assertEquals(4, bob.policy().weekendEpisodeLimit());
        assertEquals(0, bob.status().episodesWatched());

        DashboardResponse alice = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, alice.todayEvents().size());
        assertEquals(7, alice.policy().weekdayEpisodeLimit());
        assertNotEquals(bob.policy().weekdayEpisodeLimit(), alice.policy().weekdayEpisodeLimit());
    }

    @Test
    void archiveFiltersByInclusiveRangeAndOwner() {
        seed(ALICE_ID, PAST, "Past Show");
        seed(ALICE_ID, WEEKDAY, "Today Show");
        seed(ALICE_ID, FUTURE, "Future Show");
        seed(BOB_ID, WEEKDAY, "Bob Show");

        WatchEventArchiveResponse onlyPast = service.watchEventArchive(ALICE_ID, PAST, PAST);
        assertEquals(PAST, onlyPast.from());
        assertEquals(PAST, onlyPast.to());
        assertEquals(1, onlyPast.events().size());
        assertEquals("Past Show", onlyPast.events().getFirst().contentTitle());

        WatchEventArchiveResponse throughToday = service.watchEventArchive(ALICE_ID, PAST, WEEKDAY);
        assertEquals(2, throughToday.events().size());
        assertEquals("Today Show", throughToday.events().get(0).contentTitle());
        assertEquals("Past Show", throughToday.events().get(1).contentTitle());

        WatchEventArchiveResponse bob = service.watchEventArchive(BOB_ID, PAST, FUTURE);
        assertEquals(1, bob.events().size());
        assertEquals("Bob Show", bob.events().getFirst().contentTitle());
        assertEquals(BOB_ID, bob.events().getFirst().ownerId());
    }

    @Test
    void archiveOrdersByWatchedOnDescThenTitleAsc() {
        seed(ALICE_ID, FUTURE, "Zebra");
        seed(ALICE_ID, FUTURE, "Alpha");
        seed(ALICE_ID, WEEKDAY, "Mid");
        seed(ALICE_ID, PAST, "Early");

        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, PAST, FUTURE);
        assertEquals(4, archive.events().size());
        assertEquals(FUTURE, archive.events().get(0).watchedOn());
        assertEquals("Alpha", archive.events().get(0).contentTitle());
        assertEquals(FUTURE, archive.events().get(1).watchedOn());
        assertEquals("Zebra", archive.events().get(1).contentTitle());
        assertEquals(WEEKDAY, archive.events().get(2).watchedOn());
        assertEquals("Mid", archive.events().get(2).contentTitle());
        assertEquals(PAST, archive.events().get(3).watchedOn());
        assertEquals("Early", archive.events().get(3).contentTitle());
    }

    @Test
    void archiveRejectsInvertedAndOversizedRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.watchEventArchive(ALICE_ID, FUTURE, PAST)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.watchEventArchive(ALICE_ID, WEEKDAY.minusDays(366), WEEKDAY)
        );
        WatchEventArchiveResponse maxRange = service.watchEventArchive(
                ALICE_ID,
                WEEKDAY.minusDays(365),
                WEEKDAY
        );
        assertTrue(maxRange.events().isEmpty());
    }

    @Test
    void archiveDoesNotPublishAndDashboardQuotaIgnoresOtherDays() {
        seed(ALICE_ID, PAST, "Past Show");
        seed(ALICE_ID, FUTURE, "Future Show");
        service.logWatchEvent(ALICE_ID, "alice", "Today Show");

        DashboardResponse dashboard = service.dashboard(ALICE_ID, "alice");
        assertEquals(1, dashboard.status().episodesWatched());
        assertEquals(1, dashboard.todayEvents().size());
        assertEquals("Today Show", dashboard.todayEvents().getFirst().contentTitle());

        clearInvocations(integrationEventPublisher);
        WatchEventArchiveResponse archive = service.watchEventArchive(ALICE_ID, PAST, FUTURE);
        assertEquals(3, archive.events().size());
        verify(integrationEventPublisher, never()).publish(any());
    }

    private void seed(UUID ownerId, LocalDate watchedOn, String title) {
        store.appendWatchEvent(new WatchEvent(UUID.randomUUID(), ownerId, watchedOn, title));
    }
}
