package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.plannerapp.support.AuthTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/do-not-use-local-watchnest")
@AutoConfigureMockMvc
@ActiveProfiles("persistent")
@Tag("persistent-http")
@Testcontainers
class PersistentHttpApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndMeReturnSameDatabaseUser() throws Exception {
        String username = uniqueUsername("u");
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, username, "password1");

        MvcResult meAfterRegister = mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        String userId = objectMapper.readTree(meAfterRegister.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        MockHttpSession loginSession = AuthTestSupport.login(mockMvc, username, "password1");
        mockMvc.perform(get("/api/v1/auth/me").session(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loggedWatchIncreasesDashboardQuota() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("w"),
                "password1"
        );
        String ownerId = meId(session);

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Blue Tractor"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Blue Tractor"))
                .andExpect(jsonPath("$.ownerId").value(ownerId));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.episodesWatched").value(1))
                .andExpect(jsonPath("$.todayEvents[0].contentTitle").value("Blue Tractor"))
                .andExpect(jsonPath("$.todayEvents[0].ownerId").value(ownerId));
    }

    @Test
    void archiveReturnsLoggedEventForServerToday() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("a"),
                "password1"
        );

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Blue Tractor"}
                                """))
                .andExpect(status().isCreated());

        String today = dashboardToday(session);

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(today))
                .andExpect(jsonPath("$.to").value(today))
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].contentTitle").value("Blue Tractor"));
    }

    @Test
    void archiveIsolatesOwnersInDatabase() throws Exception {
        MockHttpSession aliceSession = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("alice"),
                "password1"
        );
        MockHttpSession bobSession = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("bob"),
                "password1"
        );

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(csrf())
                        .session(aliceSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Alice Show"}
                                """))
                .andExpect(status().isCreated());

        String today = dashboardToday(bobSession);

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(bobSession)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(aliceSession)
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].contentTitle").value("Alice Show"));
    }

    @Test
    void unauthenticatedArchiveRequiresSession() throws Exception {
        mockMvc.perform(get("/api/v1/watch-events")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
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

    private static String uniqueUsername(String prefix) {
        String candidate = prefix + UUID.randomUUID().toString().replace("-", "");
        return candidate.substring(0, Math.min(32, candidate.length()));
    }
}
