package dev.watchnest.plannerapp.cms.security;

import dev.watchnest.plannerapp.cms.auth.CmsCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.List;
import java.util.UUID;

final class CmsCookieCsrfTokenRepository implements CsrfTokenRepository {

    private final boolean secure;

    CmsCookieCsrfTokenRepository(boolean secure) {
        this.secure = secure;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(CmsCookies.CSRF_HEADER, "_csrf", UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null) {
            CmsCookies.clearCsrf(response, secure);
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(CmsCookies.CSRF_COOKIE, token.getToken())
                .path(CmsCookies.COOKIE_PATH)
                .httpOnly(false)
                .sameSite("Lax")
                .secure(secure)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        List<String> values = CmsCookies.nonBlankValues(request, CmsCookies.CSRF_COOKIE);
        if (values.isEmpty()) {
            return null;
        }
        String header = request.getHeader(CmsCookies.CSRF_HEADER);
        String token = values.getFirst();
        if (header != null) {
            for (String value : values) {
                if (header.equals(value)) {
                    token = value;
                    break;
                }
            }
        }
        return new DefaultCsrfToken(CmsCookies.CSRF_HEADER, "_csrf", token);
    }
}
