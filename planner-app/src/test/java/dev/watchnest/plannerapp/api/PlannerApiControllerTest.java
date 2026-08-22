package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.planner.domain.LibraryLimits;
import dev.watchnest.planner.domain.PlanLineSource;
import dev.watchnest.planner.domain.PlanToday;
import dev.watchnest.planner.domain.PlanTodayLine;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import dev.watchnest.plannerapp.library.PersonalLibraryStore;
import dev.watchnest.plannerapp.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("memory")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlannerApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersonalLibraryStore store;

    @MockitoBean
    private IntegrationEventPublisher integrationEventPublisher;

    @Test
    void dashboardReturnsPlanTodayAndQuotaForAuthenticatedUser() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("alice"))
                .andExpect(jsonPath("$.status.episodeLimit").exists())
                .andExpect(jsonPath("$.status.episodesPlanned").value(0))
                .andExpect(jsonPath("$.status.canAddAnotherEpisode").value(true))
                .andExpect(jsonPath("$.planToday.lines").isEmpty())
                .andExpect(jsonPath("$.treatPlanAsWatched").value(false))
                .andExpect(jsonPath("$.todayEvents").doesNotExist());
    }

    @Test
    void addsPlanTodayLineWithoutPublishingWatchEvent() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Blue Tractor"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Blue Tractor"))
                .andExpect(jsonPath("$.checked").value(false))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.ownerId").doesNotExist());

        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.episodesPlanned").value(1))
                .andExpect(jsonPath("$.planToday.lines[0].contentTitle").value("Blue Tractor"));
    }

    @Test
    void overQuotaPlanTodayAddStillReturnsCreated() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(put("/api/v1/policy")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":2,"weekendEpisodeLimit":2}
                                """))
                .andExpect(status().isOk());

        addTodayLine(session, "One");
        addTodayLine(session, "Two");

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Three"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.episodesPlanned").value(3))
                .andExpect(jsonPath("$.status.canAddAnotherEpisode").value(false));
    }

    @Test
    void rejectsBlankPlanTodayTitle() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void patchesAndDeletesPlanTodayLine() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String lineId = addTodayLine(session, "Toggle me");

        mockMvc.perform(patch("/api/v1/plan/today/lines/" + lineId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checked":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.contentTitle").value("Toggle me"));

        mockMvc.perform(delete("/api/v1/plan/today/lines/" + lineId).with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(jsonPath("$.planToday.lines").isEmpty())
                .andExpect(jsonPath("$.status.episodesPlanned").value(0));
    }

    @Test
    void missingCheckedOnPatchIsValidationFailed() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String lineId = addTodayLine(session, "Line");

        mockMvc.perform(patch("/api/v1/plan/today/lines/" + lineId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void unknownPlanTodayLineIsNotFound() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");

        mockMvc.perform(patch("/api/v1/plan/today/lines/" + missing)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checked":true}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void crossOwnerPlanTodayLineIsNotFound() throws Exception {
        MockHttpSession alice = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        MockHttpSession bob = AuthTestSupport.register(mockMvc, objectMapper, "bob", "password1");
        String aliceLineId = addTodayLine(alice, "Alice Show");

        mockMvc.perform(patch("/api/v1/plan/today/lines/" + aliceLineId)
                        .with(csrf())
                        .session(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checked":true}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(delete("/api/v1/plan/today/lines/" + aliceLineId).with(csrf()).session(bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void planTodayCapRejectsFiftyFirstLine() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        for (int i = 0; i < LibraryLimits.MAX_TITLES_PER_DATE; i++) {
            addTodayLine(session, "Line " + i);
        }

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Overflow"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void futurePlanTodayReturnsConflict() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        UUID ownerId = UUID.fromString(meId(session));
        LocalDate today = LocalDate.parse(dashboardToday(session));
        store.savePlanToday(new PlanToday(
                ownerId,
                today.plusDays(1),
                java.util.List.of(new PlanTodayLine(UUID.randomUUID(), "Corrupt", false, PlanLineSource.MANUAL))
        ));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_date_conflict"));
    }

    @Test
    void forwardPlanHappyPathAndRejectsTodayAndPast() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String today = dashboardToday(session);
        LocalDate future = LocalDate.parse(today).plusDays(1);

        mockMvc.perform(post("/api/v1/plan/forward")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedFor":"%s","contentTitle":"Tomorrow"}
                                """.formatted(future)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Tomorrow"))
                .andExpect(jsonPath("$.plannedFor").value(future.toString()));

        mockMvc.perform(get("/api/v1/plan/forward")
                        .session(session)
                        .param("from", today)
                        .param("to", future.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(today))
                .andExpect(jsonPath("$.to").value(future.toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].contentTitle").value("Tomorrow"));

        mockMvc.perform(post("/api/v1/plan/forward")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedFor":"%s","contentTitle":"Today"}
                                """.formatted(today)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(post("/api/v1/plan/forward")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedFor":"%s","contentTitle":"Past"}
                                """.formatted(LocalDate.parse(today).minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void forwardDeleteAndCrossOwnerNotFound() throws Exception {
        MockHttpSession alice = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        MockHttpSession bob = AuthTestSupport.register(mockMvc, objectMapper, "bob", "password1");
        String today = dashboardToday(alice);
        LocalDate future = LocalDate.parse(today).plusDays(2);

        MvcResult created = mockMvc.perform(post("/api/v1/plan/forward")
                        .with(csrf())
                        .session(alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedFor":"%s","contentTitle":"Alice Future"}
                                """.formatted(future)))
                .andExpect(status().isCreated())
                .andReturn();
        String itemId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/plan/forward/" + itemId).with(csrf()).session(bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(delete("/api/v1/plan/forward/" + itemId).with(csrf()).session(alice))
                .andExpect(status().isNoContent());
    }

    @Test
    void forwardCapRejectsFiftyFirstItemOnSameDate() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        LocalDate future = LocalDate.parse(dashboardToday(session)).plusDays(3);
        for (int i = 0; i < LibraryLimits.MAX_TITLES_PER_DATE; i++) {
            mockMvc.perform(post("/api/v1/plan/forward")
                            .with(csrf())
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"plannedFor":"%s","contentTitle":"Item %s"}
                                    """.formatted(future, i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/v1/plan/forward")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedFor":"%s","contentTitle":"Overflow"}
                                """.formatted(future)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void updatesPolicyAndPublishesIntegrationEvent() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(put("/api/v1/policy")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":3,"weekendEpisodeLimit":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekdayEpisodeLimit").value(3))
                .andExpect(jsonPath("$.weekendEpisodeLimit").value(5));

        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.ScreenTimePolicyUpdated.class));
    }

    @Test
    void rejectsPolicyOutsideAllowedRange() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(put("/api/v1/policy")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":21,"weekendEpisodeLimit":5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersAreIsolatedForPlanTodayAndPolicy() throws Exception {
        MockHttpSession aliceSession = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        MockHttpSession bobSession = AuthTestSupport.register(mockMvc, objectMapper, "bob", "password1");

        addTodayLine(aliceSession, "Alice Show");
        mockMvc.perform(put("/api/v1/policy")
                        .with(csrf())
                        .session(aliceSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":9,"weekendEpisodeLimit":10}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("bob"))
                .andExpect(jsonPath("$.planToday.lines").isEmpty())
                .andExpect(jsonPath("$.policy.weekdayEpisodeLimit").value(2))
                .andExpect(jsonPath("$.status.episodesPlanned").value(0));

        mockMvc.perform(get("/api/v1/dashboard").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planToday.lines[0].contentTitle").value("Alice Show"))
                .andExpect(jsonPath("$.policy.weekdayEpisodeLimit").value(9));
    }

    @Test
    void archiveRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/watch-events")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void archiveSameDayStaysEmptyAfterPlanTodayAdd() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        addTodayLine(session, "Blue Tractor");
        String today = dashboardToday(session);
        clearInvocations(integrationEventPublisher);

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(today))
                .andExpect(jsonPath("$.to").value(today))
                .andExpect(jsonPath("$.events").isEmpty());

        verify(integrationEventPublisher, never()).publish(any());
    }

    @Test
    void archiveIsolatesOwners() throws Exception {
        MockHttpSession aliceSession = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        MockHttpSession bobSession = AuthTestSupport.register(mockMvc, objectMapper, "bob", "password1");
        addTodayLine(aliceSession, "Alice Show");
        String today = dashboardToday(bobSession);

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(bobSession)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void archiveCorrectionAddsRenamesAndDeletesPastDays() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String today = dashboardToday(session);
        LocalDate yesterday = LocalDate.parse(today).minusDays(1);
        LocalDate tomorrow = LocalDate.parse(today).plusDays(1);

        MvcResult created = mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"%s","contentTitle":"Yesterday Show"}
                                """.formatted(yesterday)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Yesterday Show"))
                .andExpect(jsonPath("$.watchedOn").value(yesterday.toString()))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", yesterday.toString())
                        .param("to", yesterday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].id").value(id))
                .andExpect(jsonPath("$.events[0].contentTitle").value("Yesterday Show"));

        mockMvc.perform(patch("/api/v1/watch-events/" + id)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Renamed Show","watchedOn":"%s"}
                                """.formatted(tomorrow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentTitle").value("Renamed Show"))
                .andExpect(jsonPath("$.watchedOn").value(yesterday.toString()));

        mockMvc.perform(delete("/api/v1/watch-events/" + id)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", yesterday.toString())
                        .param("to", yesterday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"%s","contentTitle":"Today Show"}
                                """.formatted(today)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"%s","contentTitle":"Tomorrow Show"}
                                """.formatted(tomorrow)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void archiveMutationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"2026-07-01","contentTitle":"Nope"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));

        mockMvc.perform(patch("/api/v1/watch-events/11111111-1111-1111-1111-111111111111")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Nope"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));

        mockMvc.perform(delete("/api/v1/watch-events/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void archiveRejectsMissingAndUnparsableDates() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(get("/api/v1/watch-events").session(session).param("to", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", "not-a-date")
                        .param("to", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void archiveAndForwardRejectInvertedAndOversizedRange() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", "2026-07-10")
                        .param("to", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(get("/api/v1/plan/forward")
                        .session(session)
                        .param("from", "2026-01-01")
                        .param("to", "2027-01-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void libraryPreferencesPutUpdatesDashboardAndRejectsCheckedPatch() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String lineId = addTodayLine(session, "One");

        mockMvc.perform(put("/api/v1/library-preferences")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"treatPlanAsWatched":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatPlanAsWatched").value(true));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatPlanAsWatched").value(true))
                .andExpect(jsonPath("$.planToday.lines[0].checked").value(true));

        mockMvc.perform(patch("/api/v1/plan/today/lines/" + lineId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checked":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(put("/api/v1/library-preferences")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void libraryPreferencesRequireAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/library-preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"treatPlanAsWatched":true}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private String addTodayLine(MockHttpSession session, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String dashboardToday(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("today").asText();
    }

    private String meId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
