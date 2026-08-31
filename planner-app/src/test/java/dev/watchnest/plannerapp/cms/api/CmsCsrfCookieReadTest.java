package dev.watchnest.plannerapp.cms.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import dev.watchnest.plannerapp.support.PostgresHttpTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CmsCsrfCookieReadTest extends PostgresHttpTest {

    private static final String CSRF_COOKIE = "WATCHNEST_CMS_XSRF_TOKEN";
    private static final String CSRF_HEADER = "X-WATCHNEST-CMS-XSRF-TOKEN";
    private static final String REAL = "real-csrf-token";
    private static final String OTHER = "other-csrf-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void emptyThenRealCookieAcceptsMatchingHeader() throws Exception {
        mockMvc.perform(post("/cms/api/v1/logout")
                        .cookie(csrfCookie(""), csrfCookie(REAL))
                        .header(CSRF_HEADER, REAL))
                .andExpect(status().isNoContent());
    }

    @Test
    void realThenEmptyCookieAcceptsMatchingHeader() throws Exception {
        mockMvc.perform(post("/cms/api/v1/logout")
                        .cookie(csrfCookie(REAL), csrfCookie(""))
                        .header(CSRF_HEADER, REAL))
                .andExpect(status().isNoContent());
    }

    @Test
    void twoNonBlankCookiesAcceptHeaderMatchingTheSecond() throws Exception {
        mockMvc.perform(post("/cms/api/v1/logout")
                        .cookie(csrfCookie(REAL), csrfCookie(OTHER))
                        .header(CSRF_HEADER, OTHER))
                .andExpect(status().isNoContent());
    }

    @Test
    void emptyOnlyCookieIsCsrfInvalid() throws Exception {
        mockMvc.perform(post("/cms/api/v1/logout")
                        .cookie(csrfCookie(""))
                        .header(CSRF_HEADER, REAL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
    }

    @Test
    void headerMatchingNoneOfTheCookiesIsCsrfInvalid() throws Exception {
        mockMvc.perform(post("/cms/api/v1/logout")
                        .cookie(csrfCookie(REAL), csrfCookie(OTHER))
                        .header(CSRF_HEADER, "unrelated-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("csrf_invalid"));
    }

    private static Cookie csrfCookie(String value) {
        Cookie cookie = new Cookie(CSRF_COOKIE, value);
        cookie.setPath("/cms");
        return cookie;
    }
}
