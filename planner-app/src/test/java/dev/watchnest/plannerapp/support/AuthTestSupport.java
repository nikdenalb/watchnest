package dev.watchnest.plannerapp.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestSupport {

    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private AuthTestSupport() {
    }

    public record Csrf(Cookie cookie, String headerName, String token) {
    }

    /**
     * Cookie CSRF for MockMvc unsafe requests. Do not use
     * {@code SecurityMockMvcRequestPostProcessors.csrf()}: it replaces the
     * viewer's {@code CookieCsrfTokenRepository} on the shared {@code CsrfFilter}
     * with {@code HttpSessionCsrfTokenRepository} ({@code X-CSRF-TOKEN}), so later
     * {@code GET /api/v1/auth/csrf} no longer matches the SPA contract.
     */
    public static RequestPostProcessor spaCsrf() {
        return request -> {
            CsrfTokenRepository repository = WebTestUtils.getCsrfTokenRepository(request);
            CsrfToken token = repository.generateToken(request);
            Cookie cookie = new Cookie(CSRF_COOKIE, token.getToken());
            cookie.setPath("/");
            MockHttpServletRequest mockRequest = (MockHttpServletRequest) request;
            Cookie[] existing = mockRequest.getCookies();
            if (existing == null || existing.length == 0) {
                mockRequest.setCookies(cookie);
            } else {
                Cookie[] merged = new Cookie[existing.length + 1];
                System.arraycopy(existing, 0, merged, 0, existing.length);
                merged[existing.length] = cookie;
                mockRequest.setCookies(merged);
            }
            mockRequest.addHeader(token.getHeaderName(), token.getToken());
            return request;
        };
    }

    public static MockHttpSession register(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String username,
            String password
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(username, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username.toLowerCase()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    public static MockHttpSession login(
            MockMvc mockMvc,
            String username,
            String password
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(spaCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /**
     * SPA GET {@code /api/v1/auth/csrf}: header name, token, and one {@code XSRF-TOKEN}
     * cookie. Never uses {@code getCookies()}.
     */
    public static Csrf fetchCsrf(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String raw = result.getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(raw);
        String headerName = body.path("headerName").asText();
        String token = body.path("token").asText();
        assertEquals(CSRF_HEADER, headerName, () -> "csrf body: " + raw);
        assertFalse(token.isBlank(), () -> "csrf body: " + raw);
        Cookie cookie = xsrfCookie(result, token);
        assertNotNull(cookie, () -> "missing " + CSRF_COOKIE + "; Set-Cookie="
                + setCookieHeaders(result));
        return new Csrf(cookie, headerName, token);
    }

    /**
     * Token only (CMS independence checks). Does not require a MockMvc cookie jar.
     */
    public static String fetchCsrfToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String raw = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(raw).path("token").asText();
        assertFalse(token.isBlank(), () -> "csrf body: " + raw);
        return token;
    }

    public static String credentialsJson(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private static Cookie xsrfCookie(MvcResult result, String token) {
        Cookie cookie = result.getResponse().getCookie(CSRF_COOKIE);
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie;
        }
        String prefix = CSRF_COOKIE + "=";
        for (String header : setCookieHeaders(result)) {
            int at = header.indexOf(prefix);
            if (at < 0) {
                continue;
            }
            String value = header.substring(at + prefix.length());
            int cut = value.indexOf(';');
            if (cut >= 0) {
                value = value.substring(0, cut);
            }
            if (!value.isBlank()) {
                return new Cookie(CSRF_COOKIE, value);
            }
        }
        if (token != null && !token.isBlank()) {
            Cookie fromBody = new Cookie(CSRF_COOKIE, token);
            fromBody.setPath("/");
            return fromBody;
        }
        return null;
    }

    private static List<String> setCookieHeaders(MvcResult result) {
        List<String> headers = result.getResponse().getHeaders("Set-Cookie");
        return headers != null ? headers : new ArrayList<>();
    }
}
