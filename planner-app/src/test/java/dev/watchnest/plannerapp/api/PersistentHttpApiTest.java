package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.plannerapp.support.AuthTestSupport;
import dev.watchnest.plannerapp.support.PostgresHttpTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PersistentHttpApiTest extends PostgresHttpTest {

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

        mockMvc.perform(post("/api/v1/auth/logout").with(AuthTestSupport.spaCsrf()).session(session))
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
                        .with(AuthTestSupport.spaCsrf())
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
                        .with(AuthTestSupport.spaCsrf())
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
                        .with(AuthTestSupport.spaCsrf())
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
    void archiveCorrectionUsesDashboardTodayMinusOne() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("corr"),
                "password1"
        );
        String today = dashboardToday(session);
        LocalDate yesterday = LocalDate.parse(today).minusDays(1);
        LocalDate tomorrow = LocalDate.parse(today).plusDays(1);

        MvcResult created = mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"%s","contentTitle":"Yesterday Show"}
                                """.formatted(yesterday)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.watchedOn").value(yesterday.toString()))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", yesterday.toString())
                        .param("to", yesterday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].id").value(id));

        mockMvc.perform(patch("/api/v1/watch-events/" + id)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Renamed Show"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentTitle").value("Renamed Show"))
                .andExpect(jsonPath("$.watchedOn").value(yesterday.toString()));

        mockMvc.perform(delete("/api/v1/watch-events/" + id)
                        .with(AuthTestSupport.spaCsrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"%s","contentTitle":"Today Show"}
                                """.formatted(today)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));

        mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"watchedOn":"%s","contentTitle":"Tomorrow Show"}
                                """.formatted(tomorrow)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
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

    @Test
    void libraryPreferencesPersistAndFailedEnsureLeavesFlagUnchanged() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("pref"),
                "password1"
        );
        UUID ownerId = UUID.fromString(meId(session));
        String today = dashboardToday(session);

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatPlanAsWatched").value(false));

        mockMvc.perform(put("/api/v1/library-preferences")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"treatPlanAsWatched":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatPlanAsWatched").value(true));

        Boolean stored = jdbcTemplate.queryForObject(
                "select treat_plan_as_watched from library_profile where id = ?",
                Boolean.class,
                ownerId
        );
        assertEquals(Boolean.TRUE, stored);

        jdbcTemplate.update(
                "update plan_today set for_date = ? where owner_id = ?",
                LocalDate.parse(today).plusDays(1),
                ownerId
        );
        mockMvc.perform(put("/api/v1/library-preferences")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"treatPlanAsWatched":false}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("plan_date_conflict"));
        assertEquals(
                Boolean.TRUE,
                jdbcTemplate.queryForObject(
                        "select treat_plan_as_watched from library_profile where id = ?",
                        Boolean.class,
                        ownerId
                )
        );
    }

    @Test
    void libraryProfileOmittingTreatPlanAsWatchedColumnDefaultsFalse() throws Exception {
        String username = uniqueUsername("omit");
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, username, "password1");
        UUID ownerId = UUID.fromString(meId(session));

        jdbcTemplate.update("delete from plan_today_line where plan_today_id in (select id from plan_today where owner_id = ?)", ownerId);
        jdbcTemplate.update("delete from plan_today where owner_id = ?", ownerId);
        jdbcTemplate.update("delete from library_profile where id = ?", ownerId);
        jdbcTemplate.update(
                """
                        insert into library_profile (id, display_name, weekday_episode_limit, weekend_episode_limit)
                        values (?, ?, ?, ?)
                        """,
                ownerId,
                username,
                2,
                4
        );

        mockMvc.perform(get("/api/v1/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.treatPlanAsWatched").value(false));
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
}
