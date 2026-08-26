package dev.watchnest.plannerapp.cms.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public final class CmsSessionAuthenticationFilter extends OncePerRequestFilter {

    private final CmsSessionStore sessions;

    public CmsSessionAuthenticationFilter(CmsSessionStore sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = CmsCookies.read(request, CmsCookies.SESSION_COOKIE);
        if (token != null) {
            sessions.authenticate(token).ifPresent(session -> {
                CmsUser principal = new CmsUser(session.accountId(), session.username());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
