package dev.watchnest.plannerapp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiControllerTest extends PostgresHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IntegrationEventPublisher integrationEventPublisher;

    @Test
    void csrfEndpointIsPublic() throws Exception {
        AuthTestSupport.fetchCsrf(mockMvc, objectMapper);
    }

    @Test
    void registerCreatesSessionAndMeReturnsSameUser() throws Exception {
        String username = uniqueUsername("alice");
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, username, "password1");

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerCanonicalizesUsername() throws Exception {
        String username = uniqueUsername("bob");
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(AuthTestSupport.spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson("  " + username + "  ", "password1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        String username = uniqueUsername("alice");
        AuthTestSupport.register(mockMvc, objectMapper, username, "password1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(AuthTestSupport.spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson(username.toUpperCase(), "password2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("username_already_exists"));
    }

    @Test
    void invalidUsernameReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(AuthTestSupport.spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson("ab", "password1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void unknownAndWrongPasswordBothReturnGenericInvalidCredentials() throws Exception {
        String username = uniqueUsername("alice");
        AuthTestSupport.register(mockMvc, objectMapper, username, "password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(AuthTestSupport.spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson("nobody", "password1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(AuthTestSupport.spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson(username, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void missingCsrfOnUnsafeRequestReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson(uniqueUsername("alice"), "password1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
    }

    @Test
    void logoutInvalidatesSessionAccess() throws Exception {
        MockHttpSession session = AuthTestSupport.register(mockMvc, objectMapper, uniqueUsername("alice"), "password1");

        mockMvc.perform(post("/api/v1/auth/logout").with(AuthTestSupport.spaCsrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void logoutWithoutSessionIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(AuthTestSupport.spaCsrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedPlannerEndpointReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("authentication_required"))
                .andExpect(content().string(containsString("authentication_required")));
    }

    @Test
    void loginRestoresAccessDuringProcessLifetime() throws Exception {
        String username = uniqueUsername("alice");
        AuthTestSupport.register(mockMvc, objectMapper, username, "password1");

        MockHttpSession session = AuthTestSupport.login(mockMvc, username, "password1");

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherActuatorEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void registerPublishesNoPlannerIntegrationEvent() throws Exception {
        AuthTestSupport.register(mockMvc, objectMapper, uniqueUsername("alice"), "password1");
        verify(integrationEventPublisher, org.mockito.Mockito.never()).publish(any());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void csrfTokenFromEndpointWorksForRegister() throws Exception {
        AuthTestSupport.Csrf csrf = AuthTestSupport.fetchCsrf(mockMvc, objectMapper);
        String username = uniqueUsername("carol");
        mockMvc.perform(post("/api/v1/auth/register")
                        .header(csrf.headerName(), csrf.token())
                        .cookie(csrf.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(AuthTestSupport.credentialsJson(username, "password1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));
    }
}
