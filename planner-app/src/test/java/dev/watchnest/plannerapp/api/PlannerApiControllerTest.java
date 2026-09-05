package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.planner.domain.LibraryLimits;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import dev.watchnest.plannerapp.support.AuthTestSupport;
import dev.watchnest.plannerapp.support.PostgresHttpTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlannerApiControllerTest extends PostgresHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IntegrationEventPublisher integrationEventPublisher;

    @Test
    void rangeGetIsAuthenticatedOwnerIsolatedAndInclusive() throws Exception {
        MockHttpSession alice = registerUser("alice");
        MockHttpSession bob = registerUser("bob");
        LocalDate past = LocalDate.now().minusDays(2);
        LocalDate today = LocalDate.now();
        LocalDate future = LocalDate.now().plusDays(2);

        addEvent(alice, past, "Zebra");
        addEvent(alice, today, "Alpha");
        addEvent(alice, future, "Later");
        addEvent(bob, today, "Bob Show");

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(alice)
                        .param("from", past.toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(past.toString()))
                .andExpect(jsonPath("$.to").value(today.toString()))
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].contentTitle").value("Alpha"))
                .andExpect(jsonPath("$.events[1].contentTitle").value("Zebra"));

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(bob)
                        .param("from", today.toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].contentTitle").value("Bob Show"));
    }

    @Test
    void rangeGetRejectsInvalidAndMissingRange() throws Exception {
        MockHttpSession session = registerUser("alice");
        LocalDate from = LocalDate.of(2026, 1, 1);

        mockMvc.perform(get("/api/v1/watch-events").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", from.plusDays(1).toString())
                        .param("to", from.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", from.toString())
                        .param("to", from.plusDays(366).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void postPatchDeleteWorkForPastTodayAndFutureWithCsrf() throws Exception {
        MockHttpSession session = registerUser("alice");
        LocalDate past = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        LocalDate future = LocalDate.now().plusDays(1);

        String pastId = addEvent(session, past, "Past Show");
        String todayId = addEvent(session, today, "Today Show");
        String futureId = addEvent(session, future, "Future Show");

        mockMvc.perform(patch("/api/v1/watch-events/" + pastId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Past Renamed","watchedOn":"2099-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pastId))
                .andExpect(jsonPath("$.watchedOn").value(past.toString()))
                .andExpect(jsonPath("$.contentTitle").value("Past Renamed"));

        mockMvc.perform(patch("/api/v1/watch-events/" + todayId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Today Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedOn").value(today.toString()));

        mockMvc.perform(patch("/api/v1/watch-events/" + futureId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Future Renamed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedOn").value(future.toString()));

        mockMvc.perform(delete("/api/v1/watch-events/" + pastId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/watch-events/" + todayId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/watch-events/" + futureId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", past.toString())
                        .param("to", future.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void sameTitlePatchPublishesNoCorrectionAndMissingIdsAreNotFound() throws Exception {
        MockHttpSession session = registerUser("alice");
        LocalDate today = LocalDate.now();
        String id = addEvent(session, today, "Same");
        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));

        mockMvc.perform(patch("/api/v1/watch-events/" + id)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Same"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentTitle").value("Same"));
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventCorrected.class));

        UUID missing = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/watch-events/" + missing)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Nope"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        mockMvc.perform(delete("/api/v1/watch-events/" + missing)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        verify(integrationEventPublisher, never()).publish(any(PlannerIntegrationEvent.WatchEventDeleted.class));
    }

    @Test
    void otherOwnerCannotSeeOrMutateEvents() throws Exception {
        MockHttpSession alice = registerUser("alice");
        MockHttpSession bob = registerUser("bob");
        LocalDate today = LocalDate.now();
        String aliceId = addEvent(alice, today, "Alice Show");

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(bob)
                        .param("from", today.toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());

        mockMvc.perform(patch("/api/v1/watch-events/" + aliceId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Stolen"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
        mockMvc.perform(delete("/api/v1/watch-events/" + aliceId)
                        .with(AuthTestSupport.spaCsrf())
                        .session(bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void fiftiethEventIsCreatedAndFiftyFirstIsRejected() throws Exception {
        MockHttpSession session = registerUser("alice");
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= LibraryLimits.MAX_TITLES_PER_DATE; i++) {
            addEvent(session, today, "Title " + i);
        }
        mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(today, "Overflow")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verify(integrationEventPublisher, times(LibraryLimits.MAX_TITLES_PER_DATE))
                .publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));
    }

    @Test
    void rangeGetDoesNotCreateWatchEvents() throws Exception {
        MockHttpSession session = registerUser("alice");
        LocalDate today = LocalDate.now();
        addEvent(session, today, "Only");
        int before = countEvents(meId(session));

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", today.minusDays(30).toString())
                        .param("to", today.plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1));

        assertEquals(before, countEvents(meId(session)));
    }

    @Test
    void removedRoutesReturn404ForAuthenticatedCallerWithCsrfOnUnsafeMethods() throws Exception {
        MockHttpSession session = registerUser("alice");
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/plan/forward")
                        .session(session)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Nope"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/plan/today/lines/" + id)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checked":true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/plan/today/lines/" + id)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/plan/forward")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plannedFor":"2099-01-01","contentTitle":"Nope"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/plan/forward/" + id)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/policy")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":2,"weekendEpisodeLimit":4}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/library-preferences")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"treatPlanAsWatched":true}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void watchEventMutationsRequireCsrfAndSession() throws Exception {
        MockHttpSession session = registerUser("alice");
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/api/v1/watch-events")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(today, "No Csrf")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(today, "No Session")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private MockHttpSession registerUser(String prefix) throws Exception {
        return AuthTestSupport.register(mockMvc, objectMapper, uniqueUsername(prefix), "password1");
    }

    private String addEvent(MockHttpSession session, LocalDate watchedOn, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(watchedOn, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value(title))
                .andExpect(jsonPath("$.watchedOn").value(watchedOn.toString()))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private static String eventJson(LocalDate watchedOn, String title) {
        return """
                {"watchedOn":"%s","contentTitle":"%s"}
                """.formatted(watchedOn, title);
    }

    private String meId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private int countEvents(String ownerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from watch_event where owner_id = ?",
                Integer.class,
                UUID.fromString(ownerId)
        );
        return count == null ? 0 : count;
    }
}
