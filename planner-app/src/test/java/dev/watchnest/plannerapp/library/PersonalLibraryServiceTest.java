package dev.watchnest.plannerapp.library;

import dev.watchnest.planner.domain.LibraryLimits;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryServiceTest {

    private static final LocalDate PAST = LocalDate.of(2026, 7, 1);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 31);
    private static final LocalDate FUTURE = LocalDate.of(2026, 12, 15);
    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private InMemoryPersonalLibraryStore store;
    private PersonalLibraryService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryPersonalLibraryStore();
        service = new PersonalLibraryService(integrationEventPublisher, store);
    }

    @Test
    void rangeIsInclusiveAndOrderedWatchedOnDescThenTitleAsc() {
        service.addWatchEvent(ALICE_ID, "alice", PAST, "Zebra");
        service.addWatchEvent(ALICE_ID, "alice", TODAY, "Alpha");
        service.addWatchEvent(ALICE_ID, "alice", TODAY, "Beta");
        service.addWatchEvent(ALICE_ID, "alice", FUTURE, "Later");

        WatchEventArchiveResponse inside = service.listWatchEvents(ALICE_ID, PAST, TODAY);
        assertEquals(List.of("Alpha", "Beta", "Zebra"), titles(inside));
        assertEquals(PAST, inside.from());
        assertEquals(TODAY, inside.to());

        WatchEventArchiveResponse emptyDay = service.listWatchEvents(ALICE_ID, PAST.minusDays(1), PAST.minusDays(1));
        assertTrue(emptyDay.events().isEmpty());
    }

    @Test
    void rangeAccepts366DayInclusiveSpanAndRejectsWiderOrInverted() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate maxTo = from.plusDays(365);
        assertTrue(service.listWatchEvents(ALICE_ID, from, maxTo).events().isEmpty());

        IllegalArgumentException tooWide = assertThrows(
                IllegalArgumentException.class,
                () -> service.listWatchEvents(ALICE_ID, from, from.plusDays(366))
        );
        assertTrue(tooWide.getMessage().contains("366"));

        assertThrows(IllegalArgumentException.class, () -> service.listWatchEvents(ALICE_ID, TODAY, PAST));
        assertThrows(IllegalArgumentException.class, () -> service.listWatchEvents(ALICE_ID, null, TODAY));
        assertThrows(IllegalArgumentException.class, () -> service.listWatchEvents(ALICE_ID, TODAY, null));
        verifyNoInteractions(integrationEventPublisher);
    }

    @Test
    void ownersAreIsolatedOnRangeRead() {
        service.addWatchEvent(ALICE_ID, "alice", TODAY, "Alice Show");
        service.addWatchEvent(BOB_ID, "bob", TODAY, "Bob Show");

        List<String> alice = titles(service.listWatchEvents(ALICE_ID, TODAY, TODAY));
        List<String> bob = titles(service.listWatchEvents(BOB_ID, TODAY, TODAY));
        assertEquals(List.of("Alice Show"), alice);
        assertEquals(List.of("Bob Show"), bob);
    }

    @Test
    void addAcceptsPastTodayAndFutureDates() {
        WatchEventResponse past = service.addWatchEvent(ALICE_ID, "alice", PAST, "Past Show");
        WatchEventResponse today = service.addWatchEvent(ALICE_ID, "alice", TODAY, "Today Show");
        WatchEventResponse future = service.addWatchEvent(ALICE_ID, "alice", FUTURE, "Future Show");

        assertEquals(PAST, past.watchedOn());
        assertEquals(TODAY, today.watchedOn());
        assertEquals(FUTURE, future.watchedOn());
        assertEquals(3, service.listWatchEvents(ALICE_ID, PAST, FUTURE).events().size());
        verify(integrationEventPublisher, times(3)).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
    }

    @Test
    void addPublishesRecordedPayload() {
        WatchEventResponse created = service.addWatchEvent(ALICE_ID, "alice", TODAY, "  Blue Tractor  ");
        assertEquals("Blue Tractor", created.contentTitle());

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher).publish(captor.capture());
        PlannerIntegrationEvent.WatchEventRecorded recorded =
                (PlannerIntegrationEvent.WatchEventRecorded) captor.getValue();
        assertEquals(created.id(), recorded.watchEvent().id());
        assertEquals(ALICE_ID, recorded.watchEvent().ownerId());
        assertEquals(TODAY, recorded.watchEvent().watchedOn());
        assertEquals("Blue Tractor", recorded.watchEvent().contentTitle());
    }

    @Test
    void fiftiethEventForADateIsAcceptedAndFiftyFirstIsRejectedWithoutEvent() {
        for (int i = 1; i <= LibraryLimits.MAX_TITLES_PER_DATE; i++) {
            service.addWatchEvent(ALICE_ID, "alice", TODAY, "Title " + i);
        }
        assertEquals(
                LibraryLimits.MAX_TITLES_PER_DATE,
                service.listWatchEvents(ALICE_ID, TODAY, TODAY).events().size()
        );

        IllegalArgumentException cap = assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", TODAY, "Overflow")
        );
        assertTrue(cap.getMessage().contains("50"));
        verify(integrationEventPublisher, times(LibraryLimits.MAX_TITLES_PER_DATE))
                .publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
        assertEquals(
                LibraryLimits.MAX_TITLES_PER_DATE,
                service.listWatchEvents(ALICE_ID, TODAY, TODAY).events().size()
        );
    }

    @Test
    void titleRequiredTrimNonBlankAndMaxLength() {
        assertThrows(IllegalArgumentException.class, () -> service.addWatchEvent(ALICE_ID, "alice", TODAY, null));
        assertThrows(IllegalArgumentException.class, () -> service.addWatchEvent(ALICE_ID, "alice", TODAY, "   "));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addWatchEvent(ALICE_ID, "alice", TODAY, "x".repeat(121))
        );
        WatchEventResponse ok = service.addWatchEvent(ALICE_ID, "alice", TODAY, "x".repeat(120));
        assertEquals(120, ok.contentTitle().length());
        verify(integrationEventPublisher, times(1)).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
    }

    @Test
    void renameOnPastTodayAndFuturePreservesIdAndDate() {
        WatchEventResponse past = service.addWatchEvent(ALICE_ID, "alice", PAST, "Old Past");
        WatchEventResponse today = service.addWatchEvent(ALICE_ID, "alice", TODAY, "Old Today");
        WatchEventResponse future = service.addWatchEvent(ALICE_ID, "alice", FUTURE, "Old Future");

        WatchEventResponse renamedPast = service.patchWatchEvent(ALICE_ID, past.id(), "New Past");
        WatchEventResponse renamedToday = service.patchWatchEvent(ALICE_ID, today.id(), "New Today");
        WatchEventResponse renamedFuture = service.patchWatchEvent(ALICE_ID, future.id(), "New Future");

        assertEquals(past.id(), renamedPast.id());
        assertEquals(PAST, renamedPast.watchedOn());
        assertEquals("New Past", renamedPast.contentTitle());
        assertEquals(today.id(), renamedToday.id());
        assertEquals(TODAY, renamedToday.watchedOn());
        assertEquals(future.id(), renamedFuture.id());
        assertEquals(FUTURE, renamedFuture.watchedOn());
        verify(integrationEventPublisher, times(3)).publish(any(PlannerIntegrationEvent.WatchEventCorrected.class));
    }

    @Test
    void sameTitlePatchIsNoOpAndPublishesNothing() {
        WatchEventResponse created = service.addWatchEvent(ALICE_ID, "alice", TODAY, "Same");
        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));

        WatchEventResponse again = service.patchWatchEvent(ALICE_ID, created.id(), "  Same  ");
        assertEquals(created.id(), again.id());
        assertEquals("Same", again.contentTitle());
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventCorrected.class));
    }

    @Test
    void correctedEventPayloadUsesPreviousTitleAndUpdatedEvent() {
        WatchEventResponse created = service.addWatchEvent(ALICE_ID, "alice", TODAY, "Before");
        service.patchWatchEvent(ALICE_ID, created.id(), "After");

        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher, times(2)).publish(captor.capture());
        PlannerIntegrationEvent.WatchEventCorrected corrected =
                (PlannerIntegrationEvent.WatchEventCorrected) captor.getAllValues().get(1);
        assertEquals("Before", corrected.previousTitle());
        assertEquals(created.id(), corrected.updated().id());
        assertEquals(TODAY, corrected.updated().watchedOn());
        assertEquals("After", corrected.updated().contentTitle());
    }

    @Test
    void deleteOnPastTodayAndFuturePublishesDeletedPayload() {
        WatchEventResponse past = service.addWatchEvent(ALICE_ID, "alice", PAST, "Past Show");
        WatchEventResponse today = service.addWatchEvent(ALICE_ID, "alice", TODAY, "Today Show");
        WatchEventResponse future = service.addWatchEvent(ALICE_ID, "alice", FUTURE, "Future Show");

        service.deleteWatchEvent(ALICE_ID, past.id());
        service.deleteWatchEvent(ALICE_ID, today.id());
        service.deleteWatchEvent(ALICE_ID, future.id());

        assertTrue(service.listWatchEvents(ALICE_ID, PAST, FUTURE).events().isEmpty());
        ArgumentCaptor<PlannerIntegrationEvent> captor = ArgumentCaptor.forClass(PlannerIntegrationEvent.class);
        verify(integrationEventPublisher, times(6)).publish(captor.capture());
        List<PlannerIntegrationEvent.WatchEventDeleted> deleted = captor.getAllValues().stream()
                .filter(PlannerIntegrationEvent.WatchEventDeleted.class::isInstance)
                .map(PlannerIntegrationEvent.WatchEventDeleted.class::cast)
                .toList();
        assertEquals(3, deleted.size());
        assertEquals(past.id(), deleted.get(0).id());
        assertEquals(PAST, deleted.get(0).watchedOn());
        assertEquals("Past Show", deleted.get(0).contentTitle());
        assertEquals(ALICE_ID, deleted.get(0).ownerId());
    }

    @Test
    void missingAndOtherOwnerPatchAndDeleteAreNotFoundAndPublishNothing() {
        WatchEventResponse alice = service.addWatchEvent(ALICE_ID, "alice", TODAY, "Alice Show");
        UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");

        assertThrows(
                LibraryResourceNotFoundException.class,
                () -> service.patchWatchEvent(ALICE_ID, missing, "Nope")
        );
        assertThrows(
                LibraryResourceNotFoundException.class,
                () -> service.patchWatchEvent(BOB_ID, alice.id(), "Nope")
        );
        assertThrows(LibraryResourceNotFoundException.class, () -> service.deleteWatchEvent(ALICE_ID, missing));
        assertThrows(LibraryResourceNotFoundException.class, () -> service.deleteWatchEvent(BOB_ID, alice.id()));

        assertEquals("Alice Show", service.listWatchEvents(ALICE_ID, TODAY, TODAY).events().getFirst().contentTitle());
        verify(integrationEventPublisher, times(1)).publish(any());
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventCorrected.class));
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventDeleted.class));
    }

    @Test
    void listDoesNotPublishEvents() {
        service.addWatchEvent(ALICE_ID, "alice", TODAY, "Show");
        service.listWatchEvents(ALICE_ID, TODAY, TODAY);
        verify(integrationEventPublisher, times(1)).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventCorrected.class));
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventDeleted.class));
    }

    private static List<String> titles(WatchEventArchiveResponse response) {
        return response.events().stream().map(WatchEventResponse::contentTitle).toList();
    }
}
