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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceTest {

    private static final LocalDate WEEKDAY = LocalDate.of(2026, 7, 6);

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private PersonalLibraryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
        service = new PersonalLibraryService(clock, new ScreenTimeQuotaCalculator(), integrationEventPublisher);
    }

    @Test
    void dashboardStartsWithSeedProfileAndEmptyLog() {
        DashboardResponse dashboard = service.dashboard();

        assertEquals("You", dashboard.displayName());
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
        WatchEventResponse response = service.logWatchEvent("  Blue Tractor  ");

        assertEquals("Blue Tractor", response.contentTitle());
        assertEquals(WEEKDAY, response.watchedOn());

        DashboardResponse dashboard = service.dashboard();
        assertEquals(1, dashboard.status().episodesWatched());
        assertEquals(1, dashboard.todayEvents().size());
        assertEquals("Blue Tractor", dashboard.todayEvents().getFirst().contentTitle());

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        assertTrue(captor.getValue() instanceof PlannerIntegrationEvent.WatchEventRecorded);
    }

    @Test
    void logWatchEventRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> service.logWatchEvent("   "));
        assertThrows(IllegalArgumentException.class, () -> service.logWatchEvent(null));
    }

    @Test
    void updatePolicyChangesLimitsAndPublishesEvent() {
        service.updateScreenTimePolicy(3, 5);

        DashboardResponse dashboard = service.dashboard();
        assertEquals(3, dashboard.policy().weekdayEpisodeLimit());
        assertEquals(5, dashboard.policy().weekendEpisodeLimit());
        assertEquals(3, dashboard.status().episodeLimit());

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        assertTrue(captor.getValue() instanceof PlannerIntegrationEvent.ScreenTimePolicyUpdated);
    }

    @Test
    void todayEventsAreSortedByTitle() {
        service.logWatchEvent("Zebra");
        service.logWatchEvent("Alpha");

        assertEquals("Alpha", service.dashboard().todayEvents().get(0).contentTitle());
        assertEquals("Zebra", service.dashboard().todayEvents().get(1).contentTitle());
    }
}
