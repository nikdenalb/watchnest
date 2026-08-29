package dev.watchnest.plannerapp.cms.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.ArrayList;
import java.util.List;

public final class CmsCookies {

    public static final String SESSION_COOKIE = "WATCHNEST_CMS_SESSION";
    public static final String CSRF_COOKIE = "WATCHNEST_CMS_XSRF_TOKEN";
    public static final String CSRF_HEADER = "X-WATCHNEST-CMS-XSRF-TOKEN";
    public static final String COOKIE_PATH = "/cms";

    private CmsCookies() {
    }

    public static String read(HttpServletRequest request, String name) {
        List<String> values = nonBlankValues(request, name);
        return values.isEmpty() ? null : values.getFirst();
    }

    public static List<String> nonBlankValues(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Cookie cookie : cookies) {
            if (!name.equals(cookie.getName())) {
                continue;
            }
            String value = cookie.getValue();
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    public static void writeSession(HttpServletResponse response, String token, boolean secure) {
        add(response, sessionCookie(token, secure, -1));
    }

    public static void clearSession(HttpServletResponse response, boolean secure) {
        add(response, sessionCookie("", secure, 0));
    }

    public static void clearCsrf(HttpServletResponse response, boolean secure) {
        add(response, csrfCookie("", secure, 0));
    }

    private static ResponseCookie sessionCookie(String value, boolean secure, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(SESSION_COOKIE, value)
                .path(COOKIE_PATH)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(secure);
        if (maxAgeSeconds >= 0) {
            builder.maxAge(maxAgeSeconds);
        }
        return builder.build();
    }

    private static ResponseCookie csrfCookie(String value, boolean secure, long maxAgeSeconds) {
        return ResponseCookie.from(CSRF_COOKIE, value)
                .path(COOKIE_PATH)
                .httpOnly(false)
                .sameSite("Lax")
                .secure(secure)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private static void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
