package dev.watchnest.plannerapp.cms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.watchnest.identity.port.PasswordHasher;
import dev.watchnest.plannerapp.cms.account.CmsAccount;
import dev.watchnest.plannerapp.cms.account.InMemoryCmsAccountRepository;
import dev.watchnest.plannerapp.library.InMemoryPersonalLibraryStore;
import dev.watchnest.plannerapp.support.AuthTestSupport;
import dev.watchnest.plannerapp.support.CmsTestSupport;
import dev.watchnest.plannerapp.support.CmsTestSupport.CmsCsrf;
import dev.watchnest.plannerapp.support.CmsTestSupport.CmsSession;
import dev.watchnest.plannerapp.support.MutableClock;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("memory")
@Import(CmsAuthApiControllerTest.FixedClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CmsAuthApiControllerTest {

    private static final Instant T0 = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID EDITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryCmsAccountRepository cmsAccounts;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private MutableClock clock;

    @Autowired
    private InMemoryPersonalLibraryStore libraryStore;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(T0);
        }
    }

    @BeforeEach
    void seedEditor() {
        clock.setInstant(T0);
        cmsAccounts.seed(new CmsAccount(
                EDITOR_ID,
                CmsTestSupport.EDITOR,
                passwordHasher.hash(CmsTestSupport.PASSWORD),
                false,
                T0
        ));
    }

    @Test
    void csrfUsesCmsCookieHeaderPathAndSameSite() throws Exception {
        MvcResult result = mockMvc.perform(get("/cms/api/v1/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-WATCHNEST-CMS-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("WATCHNEST_CMS_XSRF_TOKEN");
        assertNotNull(cookie);
        assertFalse(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertTrue("/cms".equals(cookie.getPath()));
        String header = setCookie(result, "WATCHNEST_CMS_XSRF_TOKEN");
        assertTrue(header.contains("SameSite=Lax"));
        assertFalse(header.contains("HttpOnly"));
        assertFalse(header.toLowerCase().contains("secure"));
    }

    @Test
    void loginSetsCmsSessionCookieAndNeverCreatesJsessionId() throws Exception {
        MvcResult result = loginResult();
        Cookie session = result.getResponse().getCookie("WATCHNEST_CMS_SESSION");
        assertNotNull(session);
        assertTrue(session.isHttpOnly());
        assertFalse(session.getSecure());
        assertTrue("/cms".equals(session.getPath()));
        assertNull(result.getResponse().getCookie("JSESSIONID"));
        String header = setCookie(result, "WATCHNEST_CMS_SESSION");
        assertTrue(header.contains("SameSite=Lax"));
        assertTrue(header.contains("HttpOnly"));
        assertNotNull(session.getValue());
        assertTrue(session.getValue().length() >= 43);
    }

    @Test
    void cmsCookieAuthenticatesCmsMeAndViewerSessionDoesNot() throws Exception {
        CmsSession cms = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD);
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/me"), cms))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EDITOR_ID.toString()))
                .andExpect(jsonPath("$.username").value(CmsTestSupport.EDITOR))
                .andExpect(jsonPath("$.demo").doesNotExist());

        MockHttpSession viewer = AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");
        mockMvc.perform(get("/cms/api/v1/me").session(viewer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void cmsCookieIsIgnoredByViewerApi() throws Exception {
        CmsSession cms = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD);
        mockMvc.perform(get("/api/v1/auth/me").cookie(cms.session()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void demoLoginAndMeOmitDemoField() throws Exception {
        UUID demoId = UUID.fromString("00000000-0000-0000-0000-00000000000d");
        cmsAccounts.seed(new CmsAccount(
                demoId,
                CmsTestSupport.DEMO,
                passwordHasher.hash(CmsTestSupport.PASSWORD),
                true,
                T0
        ));
        CmsSession cms = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.DEMO, CmsTestSupport.PASSWORD);
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/me"), cms))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(demoId.toString()))
                .andExpect(jsonPath("$.username").value(CmsTestSupport.DEMO))
                .andExpect(jsonPath("$.demo").doesNotExist());
    }

    @Test
    void invalidUnknownAndViewerOnlyCredentialsAreGenericUnauthorized() throws Exception {
        CmsCsrf csrf = CmsTestSupport.fetchCsrf(mockMvc, objectMapper);
        AuthTestSupport.register(mockMvc, objectMapper, "alice", "password1");

        mockMvc.perform(CmsTestSupport.withCmsCsrf(post("/cms/api/v1/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson("nobody", CmsTestSupport.PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(CmsTestSupport.withCmsCsrf(post("/cms/api/v1/login"), CmsTestSupport.fetchCsrf(mockMvc, objectMapper))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson("alice", "password1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));

        mockMvc.perform(CmsTestSupport.withCmsCsrf(post("/cms/api/v1/login"), CmsTestSupport.fetchCsrf(mockMvc, objectMapper))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson(CmsTestSupport.EDITOR, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void noRegisterEndpointAndLoginDoesNotCreateLibraryProfile() throws Exception {
        CmsCsrf csrf = CmsTestSupport.fetchCsrf(mockMvc, objectMapper);
        mockMvc.perform(CmsTestSupport.withCmsCsrf(post("/cms/api/v1/register"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson("editor2", "password1")))
                .andExpect(status().is4xxClientError());

        CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD);
        assertTrue(libraryProfiles().isEmpty());
    }

    @Test
    void unauthenticatedMeAndTitlesAreUnauthorized() throws Exception {
        mockMvc.perform(get("/cms/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
        mockMvc.perform(get("/cms/api/v1/titles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void idleExpiryReloginReplacementAndIdempotentLogout() throws Exception {
        CmsSession first = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD);
        clock.advance(Duration.ofMinutes(31));
        mockMvc.perform(CmsTestSupport.withCmsAuth(get("/cms/api/v1/me"), first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));

        clock.setInstant(T0);
        CmsCsrf csrf = CmsTestSupport.fetchCsrf(mockMvc, objectMapper);
        MvcResult relogin = mockMvc.perform(CmsTestSupport.withCmsCsrf(post("/cms/api/v1/login"), csrf)
                        .cookie(first.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson(CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie replacement = relogin.getResponse().getCookie("WATCHNEST_CMS_SESSION");
        assertNotNull(replacement);
        assertNotEquals(first.session().getValue(), replacement.getValue());

        mockMvc.perform(get("/cms/api/v1/me").cookie(first.session()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/cms/api/v1/me").cookie(replacement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(CmsTestSupport.EDITOR));

        CmsSession current = new CmsSession(
                replacement,
                first.csrf(),
                first.csrfHeaderName(),
                first.csrfToken()
        );
        mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/logout"), current))
                .andExpect(status().isNoContent());
        MvcResult loggedOut = mockMvc.perform(CmsTestSupport.withCmsSession(post("/cms/api/v1/logout"), current))
                .andExpect(status().isNoContent())
                .andReturn();
        assertTrue(setCookie(loggedOut, "WATCHNEST_CMS_SESSION").contains("Max-Age=0"));
        assertTrue(setCookie(loggedOut, "WATCHNEST_CMS_XSRF_TOKEN").contains("Max-Age=0"));
        mockMvc.perform(get("/cms/api/v1/me").cookie(replacement))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cmsCsrfIsRequiredAndIndependentFromViewerXsrf() throws Exception {
        mockMvc.perform(post("/cms/api/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson(CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));

        String viewerToken = AuthTestSupport.fetchCsrfToken(mockMvc, objectMapper);
        mockMvc.perform(post("/cms/api/v1/login")
                        .header("X-XSRF-TOKEN", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson(CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));

        CmsSession session = CmsTestSupport.login(mockMvc, objectMapper, CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD);
        mockMvc.perform(post("/cms/api/v1/logout").cookie(session.session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
    }

    private MvcResult loginResult() throws Exception {
        CmsCsrf csrf = CmsTestSupport.fetchCsrf(mockMvc, objectMapper);
        return mockMvc.perform(CmsTestSupport.withCmsCsrf(post("/cms/api/v1/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CmsTestSupport.credentialsJson(CmsTestSupport.EDITOR, CmsTestSupport.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String setCookie(MvcResult result, String name) {
        List<String> headers = result.getResponse().getHeaders("Set-Cookie");
        return headers.stream()
                .filter(header -> header.startsWith(name + "="))
                .findFirst()
                .orElse("");
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, ?> libraryProfiles() throws Exception {
        Field field = InMemoryPersonalLibraryStore.class.getDeclaredField("profiles");
        field.setAccessible(true);
        return (Map<UUID, ?>) field.get(libraryStore);
    }
}
