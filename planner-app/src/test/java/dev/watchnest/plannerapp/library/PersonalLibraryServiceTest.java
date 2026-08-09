package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import dev.watchnest.plannerapp.api.dto.DashboardResponse;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceTest {

    private static final LocalDate WEEKDAY = LocalDate.of(2026, 7, 6);
    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    @Mock
    private ObjectProvider<PlatformTransactionManager> transactionManagers;

    private PersonalLibraryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
        service = new PersonalLibraryService(
                clock,
                new ScreenTimeQuotaCalculator(),
                integrationEventPublisher,
                new InMemoryPersonalLibraryStore(),
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
}
