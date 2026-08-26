package dev.watchnest.plannerapp.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class CmsTestSupport {

    public static final String EDITOR = "editor";
    public static final String PASSWORD = "password1";

    private CmsTestSupport() {
    }

    public record CmsCsrf(Cookie cookie, String headerName, String token) {
    }

    public record CmsSession(Cookie session, Cookie csrf, String csrfHeaderName, String csrfToken) {
    }

    public static CmsCsrf fetchCsrf(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult result = mockMvc.perform(get("/cms/api/v1/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-WATCHNEST-CMS-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("WATCHNEST_CMS_XSRF_TOKEN");
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CmsCsrf(cookie, body.get("headerName").asText(), body.get("token").asText());
    }

    public static CmsSession login(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String username,
            String password
    ) throws Exception {
        CmsCsrf csrf = fetchCsrf(mockMvc, objectMapper);
        MvcResult result = mockMvc.perform(withCmsCsrf(post("/cms/api/v1/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        Cookie session = result.getResponse().getCookie("WATCHNEST_CMS_SESSION");
        Cookie csrfCookie = result.getResponse().getCookie("WATCHNEST_CMS_XSRF_TOKEN");
        if (csrfCookie == null) {
            csrfCookie = csrf.cookie();
        }
        return new CmsSession(session, csrfCookie, csrf.headerName(), csrf.token());
    }

    public static MockHttpServletRequestBuilder withCmsCsrf(
            MockHttpServletRequestBuilder builder,
            CmsCsrf csrf
    ) {
        return builder.cookie(csrf.cookie()).header(csrf.headerName(), csrf.token());
    }

    public static MockHttpServletRequestBuilder withCmsSession(
            MockHttpServletRequestBuilder builder,
            CmsSession session
    ) {
        return builder.cookie(session.session(), session.csrf())
                .header(session.csrfHeaderName(), session.csrfToken());
    }

    public static MockHttpServletRequestBuilder withCmsAuth(
            MockHttpServletRequestBuilder builder,
            CmsSession session
    ) {
        return builder.cookie(session.session());
    }

    public static String credentialsJson(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    public static String titleJson(
            String type,
            String nameEn,
            String nameOriginal,
            int year,
            String description,
            String genres,
            String countries
    ) {
        return """
                {"type":"%s","nameEn":%s,"nameOriginal":%s,"year":%d,"description":%s,"genres":%s,"countries":%s}
                """.formatted(
                type,
                jsonString(nameEn),
                jsonString(nameOriginal),
                year,
                jsonString(description),
                jsonString(genres),
                jsonString(countries)
        );
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
