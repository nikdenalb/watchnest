package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockitoBean
    private IntegrationEventPublisher integrationEventPublisher;

    @Test
    void dashboardReturnsPersonalLibraryAndQuotaForAuthenticatedUser() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("alice"))
                .andExpect(jsonPath("$.status.episodeLimit").exists())
                .andExpect(jsonPath("$.status.episodesWatched").value(0))
                .andExpect(jsonPath("$.todayEvents").isEmpty());
    }

    @Test
    void logsWatchEventAndPublishesIntegrationEvent() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String userId = meId(session);

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Blue Tractor"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Blue Tractor"))
                .andExpect(jsonPath("$.ownerId").value(userId));

        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.episodesWatched").value(1))
                .andExpect(jsonPath("$.todayEvents[0].contentTitle").value("Blue Tractor"));
    }

    @Test
    void rejectsBlankWatchTitle() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"   "}
                                """))
                .andExpect(status().isBadRequest());
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

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy.weekdayEpisodeLimit").value(3))
                .andExpect(jsonPath("$.policy.weekendEpisodeLimit").value(5));
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
    void usersAreIsolatedForEventsAndPolicy() throws Exception {
        MockHttpSession aliceSession = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        MockHttpSession bobSession = AuthTestSupport.register(mockMvc, objectMapper, "bob", "password1");
        String aliceId = meId(aliceSession);

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(aliceSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Alice Show"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(aliceId));

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
                .andExpect(jsonPath("$.todayEvents").isEmpty())
                .andExpect(jsonPath("$.policy.weekdayEpisodeLimit").value(2))
                .andExpect(jsonPath("$.policy.weekendEpisodeLimit").value(4))
                .andExpect(jsonPath("$.status.episodesWatched").value(0));

        mockMvc.perform(get("/api/v1/dashboard").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("alice"))
                .andExpect(jsonPath("$.todayEvents[0].contentTitle").value("Alice Show"))
                .andExpect(jsonPath("$.policy.weekdayEpisodeLimit").value(9));
    }

    @Test
    void ownerIdCannotBeInjectedThroughWatchRequest() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        String aliceId = meId(session);

        MvcResult result = mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Show","ownerId":"99999999-9999-9999-9999-999999999999"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(aliceId, body.get("ownerId").asText());
        assertNotEquals("99999999-9999-9999-9999-999999999999", body.get("ownerId").asText());
    }

    private String meId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
