package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.planner.domain.LibraryLimits;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void diarySchemaDropsPlanningTablesAndPolicyColumns() {
        assertFalse(tableExists("plan_today_line"));
        assertFalse(tableExists("plan_today"));
        assertFalse(tableExists("forward_plan_item"));
        assertTrue(tableExists("library_profile"));
        assertTrue(tableExists("watch_event"));
        assertTrue(changeSetRan("009-diary-only"));
        assertFalse(columnExists("library_profile", "weekday_episode_limit"));
        assertFalse(columnExists("library_profile", "weekend_episode_limit"));
        assertFalse(columnExists("library_profile", "treat_plan_as_watched"));
        assertTrue(columnExists("library_profile", "id"));
        assertTrue(columnExists("library_profile", "display_name"));
        assertTrue(columnExists("watch_event", "watched_on"));
    }

    @Test
    void registerCreatesLibraryProfileAndWatchEventsStayUsable() throws Exception {
        String username = uniqueUsername("d");
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, username, "password1");
        UUID ownerId = UUID.fromString(meId(session));
        assertEquals(1, count("library_profile", ownerId));

        LocalDate today = LocalDate.now();
        mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(today, "Diary Show")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", today.toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].contentTitle").value("Diary Show"));
        assertEquals(1, countEvents(ownerId));
    }

    @Test
    void rangeGetDoesNotInventWatchEvents() throws Exception {
        MockHttpSession session = AuthTestSupport.register(
                mockMvc,
                objectMapper,
                uniqueUsername("r"),
                "password1"
        );
        UUID ownerId = UUID.fromString(meId(session));
        int before = countEvents(ownerId);
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/watch-events")
                        .session(session)
                        .param("from", today.minusDays(7).toString())
                        .param("to", today.plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());

        assertEquals(before, countEvents(ownerId));
    }

    @Test
    void concurrentAddsHonorPerDateCapUnderOwnerLock() throws Exception {
        String username = uniqueUsername("c");
        AuthTestSupport.register(mockMvc, objectMapper, username, "password1");
        MockHttpSession first = AuthTestSupport.login(mockMvc, username, "password1");
        MockHttpSession second = AuthTestSupport.login(mockMvc, username, "password1");
        UUID ownerId = UUID.fromString(meId(first));
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= LibraryLimits.MAX_TITLES_PER_DATE - 1; i++) {
            addEvent(first, today, "Seed " + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                int n = i;
                MockHttpSession session = n % 2 == 0 ? first : second;
                results.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    int status = mockMvc.perform(post("/api/v1/watch-events")
                                    .with(AuthTestSupport.spaCsrf())
                                    .session(session)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(eventJson(today, "Race " + n)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    if (status == 201) {
                        created.incrementAndGet();
                    }
                    return status;
                }));
            }
            start.countDown();
            int createdStatuses = 0;
            int rejectedStatuses = 0;
            for (Future<Integer> result : results) {
                int status = result.get(30, TimeUnit.SECONDS);
                if (status == 201) {
                    createdStatuses++;
                } else if (status == 400) {
                    rejectedStatuses++;
                }
            }
            assertEquals(1, createdStatuses);
            assertEquals(3, rejectedStatuses);
            assertEquals(1, created.get());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(LibraryLimits.MAX_TITLES_PER_DATE, countEvents(ownerId, today));
    }

    @Test
    void unauthenticatedWatchEventsRequiresSession() throws Exception {
        mockMvc.perform(get("/api/v1/watch-events")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private void addEvent(MockHttpSession session, LocalDate watchedOn, String title) throws Exception {
        mockMvc.perform(post("/api/v1/watch-events")
                        .with(AuthTestSupport.spaCsrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(watchedOn, title)))
                .andExpect(status().isCreated());
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

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*) from information_schema.tables
                        where table_schema = 'public' and table_name = ?
                        """,
                Integer.class,
                table
        );
        return Integer.valueOf(1).equals(count);
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*) from information_schema.columns
                        where table_schema = 'public' and table_name = ? and column_name = ?
                        """,
                Integer.class,
                table,
                column
        );
        return Integer.valueOf(1).equals(count);
    }

    private boolean changeSetRan(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id = ?",
                Integer.class,
                id
        );
        return Integer.valueOf(1).equals(count);
    }

    private int count(String table, UUID ownerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where id = ?",
                Integer.class,
                ownerId
        );
        return count == null ? 0 : count;
    }

    private int countEvents(UUID ownerId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from watch_event where owner_id = ?",
                Integer.class,
                ownerId
        );
        return count == null ? 0 : count;
    }

    private int countEvents(UUID ownerId, LocalDate watchedOn) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from watch_event where owner_id = ? and watched_on = ?",
                Integer.class,
                ownerId,
                watchedOn
        );
        return count == null ? 0 : count;
    }
}
