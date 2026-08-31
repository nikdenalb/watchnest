package dev.watchnest.plannerapp.cms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.catalog.port.CatalogIntegrationEvent;
import dev.watchnest.catalog.port.CatalogIntegrationEventPublisher;
import dev.watchnest.catalog.port.CatalogTitleCreatedV1;
import dev.watchnest.catalog.port.CatalogTitleDeletedV1;
import dev.watchnest.catalog.port.CatalogTitleUpdatedV1;
import dev.watchnest.identity.port.PasswordHasher;
import dev.watchnest.plannerapp.support.CmsTestSupport;
import dev.watchnest.plannerapp.support.CmsTestSupport.CmsSession;
import dev.watchnest.plannerapp.support.PostgresHttpTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CmsTitleApiControllerTest extends PostgresHttpTest {

    private static final UUID EDITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID DEMO_ID = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordHasher passwordHasher;

    @MockitoBean
    private CatalogIntegrationEventPublisher catalogEvents;

    private CmsSession session;

    @BeforeEach
    void seedEditorAndLogin() throws Exception {
        deleteCmsAccountsAndCatalogTitles();
        CmsTestSupport.insertCmsAccount(
                jdbcTemplate,
                passwordHasher,
                EDITOR_ID,
                CmsTestSupport.EDITOR,
                CmsTestSupport.PASSWORD,
                false
        );
        session = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD);
        reset(catalogEvents);
    }

    @Test
    void createGetListUpdateDeleteRoundTrip() throws Exception {
        MvcResult created = mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson(
                                "FILM",
                                "  Dune  ",
                                "  Dune  ",
                                2021,
                                "  Epic  ",
                                " Drama, Science Fiction ",
                                " United States, Canada "
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/cms/api/v1/titles/")))
                .andExpect(jsonPath("$.type").value("FILM"))
                .andExpect(jsonPath("$.nameEn").value("Dune"))
                .andExpect(jsonPath("$.nameOriginal").value("Dune"))
                .andExpect(jsonPath("$.year").value(2021))
                .andExpect(jsonPath("$.description").value("Epic"))
                .andExpect(jsonPath("$.genres").value("drama, science fiction"))
                .andExpect(jsonPath("$.countries").value("united states, canada"))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        verify(catalogEvents, times(1)).publish(any(CatalogTitleCreatedV1.class));

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles/" + id), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Epic"));

        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + id), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson(
                                "FILM",
                                "Dune",
                                "Dune: Part One",
                                2021,
                                null,
                                "",
                                ""
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameOriginal").value("Dune: Part One"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.genres").value(nullValue()))
                .andExpect(jsonPath("$.countries").value(nullValue()));
        verify(catalogEvents, times(1)).publish(any(CatalogTitleUpdatedV1.class));

        mockMvc.perform(CmsTestSupport.withCmsSession(delete("/cms/api/v1/titles/" + id), session))
                .andExpect(status().isNoContent());
        verify(catalogEvents, times(1)).publish(any(CatalogTitleDeletedV1.class));
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles/" + id), session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void listSearchesAndSortsAndTreatsPercentAsLiteral() throws Exception {
        createTitle("FILM", "Beta", 2020);
        createTitle("TV_SERIES", "Alpha", 2021);
        createTitle("FILM", "Alpha", 2020);
        createTitle("FILM", "100%", 1999);

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles", hasSize(4)))
                .andExpect(jsonPath("$.titles[0].nameEn").value("100%"))
                .andExpect(jsonPath("$.titles[1].nameEn").value("Alpha"))
                .andExpect(jsonPath("$.titles[1].year").value(2020))
                .andExpect(jsonPath("$.titles[2].nameEn").value("Alpha"))
                .andExpect(jsonPath("$.titles[2].type").value("TV_SERIES"))
                .andExpect(jsonPath("$.titles[3].nameEn").value("Beta"));

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles").param("q", " alp "), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles", hasSize(2)));

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles").param("q", "%"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles", hasSize(1)))
                .andExpect(jsonPath("$.titles[0].nameEn").value("100%"));
    }

    @Test
    void validationMissingAndConflictPublishNoneOnReject() throws Exception {
        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "  ", "Dune", 2021, null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verify(catalogEvents, never()).publish(any(CatalogIntegrationEvent.class));

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles/" + UUID.randomUUID()), session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        createTitle("FILM", "Dune", 2021);
        reset(catalogEvents);
        MvcResult conflict = mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "DUNE", "Other", 2021, null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("title_already_exists"))
                .andExpect(jsonPath("$.message").value(
                        "A title with the same English name, year, and type already exists"))
                .andExpect(jsonPath("$.existingTitle.nameEn").value("Dune"))
                .andExpect(jsonPath("$.existingTitle.year").value(2021))
                .andReturn();
        verify(catalogEvents, never()).publish(any());
        String existingId = objectMapper.readTree(conflict.getResponse().getContentAsString())
                .get("existingTitle").get("id").asText();

        createTitle("FILM", "Other", 2020);
        reset(catalogEvents);
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + existingId), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Other", "Other", 2020, null, null, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingTitle.nameEn").value("Other"));
        verify(catalogEvents, never()).publish(any());
    }

    @Test
    void titleWritesRequireCmsCsrf() throws Exception {
        mockMvc.perform(post("/cms/api/v1/titles")
                        .cookie(session.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Dune", "Dune", 2021, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
        verify(catalogEvents, never()).publish(any());
    }

    @Test
    void demoAccountCanReadTitlesAndRejectedWritesLeaveCatalogUnchanged() throws Exception {
        MvcResult created = mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Dune", "Dune", 2021, null, null, null)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        reset(catalogEvents);

        CmsSession demo = loginDemo();
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles"), demo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles", hasSize(1)))
                .andExpect(jsonPath("$.titles[0].id").value(id));
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles/" + id), demo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameEn").value("Dune"));

        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Other", "Other", 2020, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"))
                .andExpect(jsonPath("$.message").value(CmsDemoAccountException.MESSAGE));
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + id), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Dune", "Changed", 2021, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"))
                .andExpect(jsonPath("$.message").value(CmsDemoAccountException.MESSAGE));
        mockMvc.perform(CmsTestSupport.withCmsSession(delete("/cms/api/v1/titles/" + id), demo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"))
                .andExpect(jsonPath("$.message").value(CmsDemoAccountException.MESSAGE));

        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/titles"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titles", hasSize(1)))
                .andExpect(jsonPath("$.titles[0].nameOriginal").value("Dune"));
        verify(catalogEvents, never()).publish(any(CatalogIntegrationEvent.class));
    }

    @Test
    void demoAccountMalformedAndMissingFieldsStayValidationFailed() throws Exception {
        CmsSession demo = loginDemo();
        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + UUID.randomUUID()), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"FILM\",\"nameOriginal\":\"Dune\",\"year\":2021}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + UUID.randomUUID()), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", null, "Dune", 2021, null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
        verify(catalogEvents, never()).publish(any(CatalogIntegrationEvent.class));
    }

    @Test
    void demoAccountDomainInvalidPayloadsAndUnknownIdsAreDemoForbidden() throws Exception {
        CmsSession demo = loginDemo();
        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "  ", "Dune", 2021, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"))
                .andExpect(jsonPath("$.message").value(CmsDemoAccountException.MESSAGE));
        UUID missing = UUID.randomUUID();
        mockMvc.perform(CmsTestSupport.withCmsSession(put("/cms/api/v1/titles/" + missing), demo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Dune", "Dune", 2021, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"));
        mockMvc.perform(CmsTestSupport.withCmsSession(delete("/cms/api/v1/titles/" + missing), demo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("demo_account"));
        verify(catalogEvents, never()).publish(any(CatalogIntegrationEvent.class));
    }

    @Test
    void demoAccountMissingCsrfIsCsrfInvalid() throws Exception {
        CmsSession demo = loginDemo();
        mockMvc.perform(post("/cms/api/v1/titles")
                        .cookie(demo.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson("FILM", "Dune", "Dune", 2021, null, null, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
        verify(catalogEvents, never()).publish(any());
    }

    private CmsSession loginDemo() throws Exception {
        CmsTestSupport.insertCmsAccount(
                jdbcTemplate,
                passwordHasher,
                DEMO_ID,
                CmsTestSupport.DEMO,
                CmsTestSupport.PASSWORD,
                true
        );
        CmsSession demo = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.DEMO, CmsTestSupport.PASSWORD);
        reset(catalogEvents);
        return demo;
    }

    private void createTitle(String type, String nameEn, int year) throws Exception {
        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/titles"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.titleJson(type, nameEn, nameEn, year, null, null, null)))
                .andExpect(status().isCreated());
    }
}
