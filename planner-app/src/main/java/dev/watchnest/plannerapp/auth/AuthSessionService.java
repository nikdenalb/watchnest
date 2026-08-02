package dev.watchnest.plannerapp.auth;

import dev.watchnest.identity.domain.AuthenticatedUser;
import dev.watchnest.identity.service.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

    private final IdentityService identityService;
    private final SecurityContextRepository securityContextRepository;

    public AuthSessionService(
            IdentityService identityService,
            SecurityContextRepository securityContextRepository
    ) {
        this.identityService = identityService;
        this.securityContextRepository = securityContextRepository;
    }

    public AuthenticatedUser register(
            String username,
            String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser user = identityService.register(username, password);
        establishSession(user, request, response);
        return user;
    }

    public AuthenticatedUser login(
            String username,
            String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser user = identityService.authenticate(username, password);
        establishSession(user, request, response);
        return user;
    }

    private void establishSession(
            AuthenticatedUser user,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        WatchNestUser principal = new WatchNestUser(user.id(), user.username());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true);
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
    }
}
