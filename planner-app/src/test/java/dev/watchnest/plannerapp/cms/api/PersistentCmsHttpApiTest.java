package dev.watchnest.plannerapp.cms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.identity.port.PasswordHasher;
import dev.watchnest.plannerapp.support.CmsTestSupport;
import dev.watchnest.plannerapp.support.CmsTestSupport.CmsSession;
import dev.watchnest.plannerapp.support.PostgresHttpTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PersistentCmsHttpApiTest extends PostgresHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void migrationsCreateCmsAndCatalogTablesWithoutLibraryFks() {
        assertTrue(tableExists("cms_account"));
        assertTrue(tableExists("catalog_title"));
        assertFalse(tableExists("cms_session"));
        assertEquals(
                Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        """
                                select count(*) from information_schema.table_constraints
                                where table_schema = 'public'
                                  and table_name in ('cms_account', 'catalog_title')
                                  and constraint_type = 'FOREIGN KEY'
                                """,
                        Integer.class
                )
        );
        List<String> cmsChecks = constraintNames("cms_account");
        assertTrue(cmsChecks.contains("ck_cms_account_username"));
        assertTrue(cmsChecks.contains("ck_cms_account_password_hash"));
        assertTrue(cmsChecks.contains("uk_cms_account_username"));
        List<String> titleChecks = constraintNames("catalog_title");
        assertTrue(titleChecks.contains("ck_catalog_title_type"));
        assertTrue(titleChecks.contains("ck_catalog_title_year"));
        assertTrue(titleChecks.contains("uk_catalog_title_natural_key"));
        assertTrue(changeSetRan("006-cms-account"));
        assertTrue(changeSetRan("007-catalog-title"));
        assertTrue(changeSetRan("008-cms-account-demo"));
        assertTrue(changeSetRan("005-library-preferences"));
        assertEquals(
                "NO",
                jdbcTemplate.queryForObject(
                        """
                                select is_nullable from information_schema.columns
                                where table_schema = 'public'
                                  and table_name = 'cms_account'
                                  and column_name = 'demo'
                                """,
                        String.class
                )
        );
        String demoDefault = jdbcTemplate.queryForObject(
                """
                        select column_default from information_schema.columns
                        where table_schema = 'public'
                          and table_name = 'cms_account'
                          and column_name = 'demo'
                        """,
                String.class
        );
        assertNotNull(demoDefault);
        assertTrue(demoDefault.toLowerCase().contains("false"));
    }

    @Test
    void fixtureAccountCanLoginAndDoesNotCreateLibraryProfile() throws Exception {
        String username = uniqueUsername("ed");
        insertCmsAccountOmittingDemo(username, "password1");
        assertEquals(
                Boolean.FALSE,
                jdbcTemplate.queryForObject(
                        "select demo from cms_account where username = ?",
                        Boolean.class,
                        username
                )
        );
        int profilesBefore = count("library_profile");

        CmsSession session = CmsTestSupport.login(mockMvc, objectMapper, username, "password1");
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/me"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.demo").doesNotExist());
        assertEquals(profilesBefore, count("library_profile"));
    }

    @Test
    void durableCrudSearchAndUniqueCollision() throws Exception {
        jdbcTemplate.update("delete from catalog_title");
        String username = uniqueUsername("cat");
        insertCmsAccountOmittingDemo(username, "password1");
        CmsSession session = CmsTestSupport.login(mockMvc, objectMapper, username, "password1");

        MvcResult created = mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson(
                                "FILM",
                                "Dune",
                                "Dune",
                                2021,
                                null,
                                "Drama",
                                "United States"
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        assertEquals(Integer.valueOf(1), count("catalog_title"));

        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "DUNE", "Other", 2021, null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("title_already_exists"))
                .andExpect(jsonPath("$.existingTitle.id").value(id));
        assertEquals(Integer.valueOf(1), count("catalog_title"));

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles").param("q", "une"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles[0].nameEn").value("Dune"))
                .andExpect(jsonPath("$.titles[0].genres").value("drama"));

        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + id), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Dune", "Dune: Part One", 2021, "Epic", null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameOriginal").value("Dune: Part One"));

        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Other", "Other", 2020, null, null, null)))
                .andExpect(status().isCreated());
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + id), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Other", "Other", 2020, null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingTitle.nameEn").value("Other"));
        assertEquals(Integer.valueOf(2), count("catalog_title"));
    }

    @Test
    void demoAccountReadsTitlesAndCannotWrite() throws Exception {
        String username = uniqueUsername("dm");
        insertCmsAccount(username, "password1", true);
        CmsSession session = CmsTestSupport.login(mockMvc, objectMapper, username, "password1");
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/me"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.demo").doesNotExist());
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles").isArray());
        int titlesBefore = count("catalog_title");

        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Demo Film", "Demo Film", 2020, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"))
                .andExpect(jsonPath("$.message").value(CmsDemoAccountException.MESSAGE));
        UUID missing = UUID.randomUUID();
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + missing), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Demo Film", "Demo Film", 2020, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"));
        mockMvc.perform(CmsTestSupport.withCmsSession(delete("/cms/api/v1/titles/" + missing), session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"));
        assertEquals(titlesBefore, count("catalog_title"));
    }

    private void insertCmsAccountOmittingDemo(String username, String password) {
        jdbcTemplate.update(
                """
                        insert into cms_account (id, username, password_hash, created_at)
                        values (?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                username,
                passwordHasher.hash(password),
                Timestamp.from(Instant.parse("2026-08-25T00:00:00Z"))
        );
    }

    private void insertCmsAccount(String username, String password, boolean demo) {
        jdbcTemplate.update(
                """
                        insert into cms_account (id, username, password_hash, demo, created_at)
                        values (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                username,
                passwordHasher.hash(password),
                demo,
                Timestamp.from(Instant.parse("2026-08-25T00:00:00Z"))
        );
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

    private List<String> constraintNames(String table) {
        return jdbcTemplate.queryForList(
                """
                        select conname from pg_constraint
                        where conrelid = ?::regclass
                        """,
                String.class,
                table
        );
    }

    private boolean changeSetRan(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id = ?",
                Integer.class,
                id
        );
        return Integer.valueOf(1).equals(count);
    }

    private Integer count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }
}
