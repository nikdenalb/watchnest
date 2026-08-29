package dev.watchnest.plannerapp.cms.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CmsCookiesTest {

    @Test
    void readSkipsBlankCookiesAndReturnsFirstNonBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CmsCookies.SESSION_COOKIE, ""),
                new Cookie(CmsCookies.SESSION_COOKIE, "   "),
                new Cookie(CmsCookies.SESSION_COOKIE, "real-session"),
                new Cookie(CmsCookies.SESSION_COOKIE, "later-session")
        );

        assertEquals("real-session", CmsCookies.read(request, CmsCookies.SESSION_COOKIE));
    }

    @Test
    void readReturnsNullWhenEveryCookieIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CmsCookies.CSRF_COOKIE, ""), new Cookie(CmsCookies.CSRF_COOKIE, " "));

        assertNull(CmsCookies.read(request, CmsCookies.CSRF_COOKIE));
    }
}
