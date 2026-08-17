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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void addedPlanTodayLineIncreasesDashboardQuota() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("w"),
                "password1"
        );

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Blue Tractor"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Blue Tractor"));

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.episodesPlanned").value(1))
                .andExpect(jsonPath("$.planToday.lines[0].contentTitle").value("Blue Tractor"));
    }

    @Test
    void archiveSameDayDoesNotIncludePlanTodayLine() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("a"),
                "password1"
        );

        mockMvc.perform(post("/api/v1/plan/today/lines")
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
                .andExpect(jsonPath("$.events").isEmpty());
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

        mockMvc.perform(post("/api/v1/plan/today/lines")
                        .with(csrf())
                        .session(aliceSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Alice Show"}
                                """))
                .andExpect(status().isCreated());

        String today = dashboardToday(bobSession);

        mockMvc.perform(get("/api/v1/dashboard").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planToday.lines").isEmpty());
        mockMvc.perform(get("/api/v1/dashboard").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planToday.lines[0].contentTitle").value("Alice Show"));

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
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void unauthenticatedArchiveRequiresSession() throws Exception {
        mockMvc.perform(get("/api/v1/watch-events")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void concurrentInitialDashboardCreatesSinglePlanToday() throws Exception {
        String username = uniqueUsername("c");
        AuthTestSupport.register(mockMvc, objectMapper, username, "password1");
        MockHttpSession first = AuthTestSupport.login(mockMvc, username, "password1");
        MockHttpSession second = AuthTestSupport.login(mockMvc, username, "password1");
        UUID ownerId = UUID.fromString(meId(first));

        runConcurrentDashboards(first, second);

        Integer plans = jdbcTemplate.queryForObject(
                "select count(*) from plan_today where owner_id = ?",
                Integer.class,
                ownerId
        );
        assertEquals(1, plans);
    }

    @Test
    void concurrentDashboardFlushesStalePlanTodayOnce() throws Exception {
        String username = uniqueUsername("r");
        AuthTestSupport.register(mockMvc, objectMapper, username, "password1");
        MockHttpSession first = AuthTestSupport.login(mockMvc, username, "password1");
        MockHttpSession second = AuthTestSupport.login(mockMvc, username, "password1");
        UUID ownerId = UUID.fromString(meId(first));
        LocalDate closedDate = LocalDate.now().minusDays(1);
        UUID planId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        jdbcTemplate.update(
                "insert into plan_today (id, owner_id, for_date) values (?, ?, ?)",
                planId,
                ownerId,
                closedDate
        );
        jdbcTemplate.update(
                """
                        insert into plan_today_line (id, plan_today_id, content_title, checked, source, sort_index)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                lineId,
                planId,
                "Stale watched",
                true,
                "MANUAL",
                0
        );

        runConcurrentDashboards(first, second);

        Integer flushed = jdbcTemplate.queryForObject(
                "select count(*) from watch_event where owner_id = ? and watched_on = ? and content_title = ?",
                Integer.class,
                ownerId,
                closedDate,
                "Stale watched"
        );
        assertEquals(1, flushed);
        Integer todayPlans = jdbcTemplate.queryForObject(
                "select count(*) from plan_today where owner_id = ? and for_date = ?",
                Integer.class,
                ownerId,
                LocalDate.now()
        );
        assertEquals(1, todayPlans);
    }

    private void runConcurrentDashboards(MockHttpSession first, MockHttpSession second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (MockHttpSession session : List.of(first, second)) {
                results.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return mockMvc.perform(get("/api/v1/dashboard").session(session))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertEquals(200, result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
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
